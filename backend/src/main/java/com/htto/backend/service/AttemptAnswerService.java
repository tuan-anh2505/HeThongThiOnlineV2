package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AttemptAnswer;
import com.htto.backend.domain.DomainEnums.AttemptAnswerStatus;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.embedded.AttemptAnswerValue;
import com.htto.backend.domain.embedded.AttemptMatchingPairAnswer;
import com.htto.backend.domain.embedded.ExamAttemptMatchingSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptOptionSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import com.htto.backend.dto.request.AttemptAnswerSaveRequest;
import com.htto.backend.dto.request.AttemptMatchingPairAnswerRequest;
import com.htto.backend.dto.response.AttemptAnswerSaveResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AttemptAnswerRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.StudentProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttemptAnswerService {

    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;

    public AttemptAnswerService(
            AttemptAnswerRepository attemptAnswerRepository,
            ExamAttemptRepository examAttemptRepository,
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository
    ) {
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    public AttemptAnswerSaveResponse saveAnswer(
            String attemptId,
            String questionIdFromPath,
            AttemptAnswerSaveRequest request,
            String username
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer payload is required");
        }
        String questionId = resolveQuestionId(questionIdFromPath, request);
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = getOwnedInProgressAttempt(attemptId, student.getId());
        ExamAttemptQuestionSnapshot snapshot = getQuestionSnapshot(attempt, questionId);
        AttemptAnswerValue answerValue = buildAnswerValue(snapshot, request);

        AttemptAnswer answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(AttemptAnswer::new);
        answer.setAttemptId(attempt.getId());
        answer.setExamId(attempt.getExamId());
        answer.setQuestionId(questionId);
        answer.setStudentAnswer(answerValue);
        answer.setCorrectAnswerSnapshot(Map.of());
        answer.setScoreAchieved(null);
        answer.setIsCorrect(null);
        answer.setStatus(AttemptAnswerStatus.NOT_GRADED);

        AttemptAnswer saved = attemptAnswerRepository.save(answer);
        return new AttemptAnswerSaveResponse(true, saved.getUpdatedAt() == null ? Instant.now() : saved.getUpdatedAt());
    }

    private String resolveQuestionId(String questionIdFromPath, AttemptAnswerSaveRequest request) {
        if (StringUtils.hasText(questionIdFromPath)) {
            if (StringUtils.hasText(request.questionId()) && !questionIdFromPath.equals(request.questionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question id does not match request path");
            }
            return questionIdFromPath;
        }
        if (!StringUtils.hasText(request.questionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question id is required");
        }
        return request.questionId();
    }

    private ExamAttempt getOwnedInProgressAttempt(String attemptId, String studentId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam attempt not found"));
        refreshExpiredAttempt(attempt);
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam attempt is not in progress");
        }
        return attempt;
    }

    private void refreshExpiredAttempt(ExamAttempt attempt) {
        Instant now = Instant.now();
        if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS
                && attempt.getDeadline() != null
                && !now.isBefore(attempt.getDeadline())) {
            attempt.setStatus(ExamAttemptStatus.EXPIRED);
            examAttemptRepository.save(attempt);
        }
    }

    private ExamAttemptQuestionSnapshot getQuestionSnapshot(ExamAttempt attempt, String questionId) {
        return attempt.getQuestionSnapshots()
                .stream()
                .filter(snapshot -> questionId.equals(snapshot.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Question does not belong to this attempt"
                ));
    }

    private AttemptAnswerValue buildAnswerValue(ExamAttemptQuestionSnapshot snapshot, AttemptAnswerSaveRequest request) {
        if (snapshot.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question type is missing");
        }
        return switch (snapshot.getType()) {
            case TRUE_FALSE -> buildTrueFalseAnswer(request);
            case MULTIPLE_CHOICE -> buildMultipleChoiceAnswer(snapshot, request);
            case FILL_BLANK -> buildFillBlankAnswer(request);
            case MATCHING -> buildMatchingAnswer(snapshot, request);
        };
    }

    private AttemptAnswerValue buildTrueFalseAnswer(AttemptAnswerSaveRequest request) {
        Boolean value = request.studentAnswer() != null && request.studentAnswer().trueFalseAnswer() != null
                ? request.studentAnswer().trueFalseAnswer()
                : request.trueFalseAnswer();
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TRUE_FALSE answer must be true or false");
        }
        AttemptAnswerValue answer = new AttemptAnswerValue();
        answer.setTrueFalseAnswer(value);
        return answer;
    }

    private AttemptAnswerValue buildMultipleChoiceAnswer(
            ExamAttemptQuestionSnapshot snapshot,
            AttemptAnswerSaveRequest request
    ) {
        String selectedOptionId = request.studentAnswer() != null
                && StringUtils.hasText(request.studentAnswer().selectedOptionId())
                ? request.studentAnswer().selectedOptionId()
                : request.selectedOptionId();
        if (!StringUtils.hasText(selectedOptionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected option id is required");
        }

        Set<String> optionIds = snapshot.getOptions() == null
                ? Set.of()
                : snapshot.getOptions()
                        .stream()
                        .map(ExamAttemptOptionSnapshot::getOptionId)
                        .collect(java.util.stream.Collectors.toSet());
        if (!optionIds.contains(selectedOptionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected option does not belong to question");
        }

        AttemptAnswerValue answer = new AttemptAnswerValue();
        answer.setSelectedOptionId(selectedOptionId);
        return answer;
    }

    private AttemptAnswerValue buildFillBlankAnswer(AttemptAnswerSaveRequest request) {
        String text = request.studentAnswer() != null && request.studentAnswer().fillBlankText() != null
                ? request.studentAnswer().fillBlankText()
                : request.fillBlankText();
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fill blank answer text is required");
        }

        AttemptAnswerValue answer = new AttemptAnswerValue();
        answer.setFillBlankText(text);
        return answer;
    }

    private AttemptAnswerValue buildMatchingAnswer(
            ExamAttemptQuestionSnapshot snapshot,
            AttemptAnswerSaveRequest request
    ) {
        List<AttemptMatchingPairAnswerRequest> pairRequests = request.studentAnswer() != null
                && request.studentAnswer().matchingPairs() != null
                ? request.studentAnswer().matchingPairs()
                : request.matchingPairs();
        if (pairRequests == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching answer pairs are required");
        }
        ExamAttemptMatchingSnapshot matching = snapshot.getMatching();
        if (matching == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching data is missing");
        }

        Set<String> validLeftIds = matching.getLeftItems() == null
                ? Set.of()
                : matching.getLeftItems()
                        .stream()
                        .map(item -> item.getMatchingId())
                        .collect(java.util.stream.Collectors.toSet());
        Set<String> validRightTexts = matching.getRightItems() == null
                ? Set.of()
                : matching.getRightItems()
                        .stream()
                        .map(item -> item.getRightText())
                        .collect(java.util.stream.Collectors.toSet());
        Set<String> seenLeftIds = new HashSet<>();
        List<AttemptMatchingPairAnswer> answers = new ArrayList<>();

        for (AttemptMatchingPairAnswerRequest pairRequest : pairRequests) {
            if (pairRequest == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching pair is invalid");
            }
            String leftId = resolveLeftId(pairRequest);
            String rightText = pairRequest.rightText();
            if (!StringUtils.hasText(leftId) || !StringUtils.hasText(rightText)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching pair requires leftId and rightText");
            }
            if (!validLeftIds.contains(leftId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching left item does not belong to question");
            }
            if (!validRightTexts.contains(rightText)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching right item does not belong to question");
            }
            if (!seenLeftIds.add(leftId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matching answer contains duplicate left item");
            }

            AttemptMatchingPairAnswer answerPair = new AttemptMatchingPairAnswer();
            answerPair.setLeftId(leftId);
            answerPair.setRightText(rightText);
            answers.add(answerPair);
        }

        AttemptAnswerValue answer = new AttemptAnswerValue();
        answer.setMatchingPairs(answers);
        return answer;
    }

    private String resolveLeftId(AttemptMatchingPairAnswerRequest pairRequest) {
        if (StringUtils.hasText(pairRequest.leftId())) {
            return pairRequest.leftId();
        }
        return pairRequest.matchingId();
    }

    private StudentProfile getCurrentStudent(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (account.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student role is required");
        }
        return studentProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
    }
}
