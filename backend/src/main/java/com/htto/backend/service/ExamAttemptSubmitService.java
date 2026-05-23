package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AttemptAnswer;
import com.htto.backend.domain.DomainEnums.AttemptAnswerStatus;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.embedded.AnswerDefinition;
import com.htto.backend.domain.embedded.AnswerOption;
import com.htto.backend.domain.embedded.AttemptAnswerValue;
import com.htto.backend.domain.embedded.AttemptMatchingPairAnswer;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import com.htto.backend.domain.embedded.FillBlankAnswer;
import com.htto.backend.domain.embedded.MatchingPair;
import com.htto.backend.dto.response.ExamAttemptStatusResponse;
import com.htto.backend.dto.response.SubmitAttemptResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AttemptAnswerRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.StudentProfileRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamAttemptSubmitService {

    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{M}+");

    private final ExamAttemptRepository examAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SystemLogService systemLogService;

    public ExamAttemptSubmitService(
            ExamAttemptRepository examAttemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            ExamRepository examRepository,
            QuestionRepository questionRepository,
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository,
            SystemLogService systemLogService
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.systemLogService = systemLogService;
    }

    public SubmitAttemptResponse submitAttempt(String attemptId, String username) {
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = getOwnedAttempt(attemptId, student.getId());
        Exam exam = getExamOrThrow(attempt.getExamId());

        if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS && isExpired(attempt, Instant.now())) {
            ExamAttempt autoSubmitted = autoSubmitExpiredAttempt(attempt, exam, student.getAccountId());
            return toSubmitResponse(autoSubmitted, exam);
        }
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam attempt is not in progress");
        }

        ExamAttempt submitted = gradeAndCloseAttempt(attempt, exam, ExamAttemptStatus.SUBMITTED);
        systemLogService.log(student.getAccountId(), "SUBMIT_EXAM", "EXAM_ATTEMPT", submitted.getId(), "Student submitted exam");
        return toSubmitResponse(submitted, exam);
    }

    public SubmitAttemptResponse autoSubmitAttempt(String attemptId, String username) {
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = getOwnedAttempt(attemptId, student.getId());
        Exam exam = getExamOrThrow(attempt.getExamId());

        if (attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
            if (!isExpired(attempt, Instant.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam attempt has not expired");
            }
            attempt = autoSubmitExpiredAttempt(attempt, exam, student.getAccountId());
        }
        return toSubmitResponse(attempt, exam);
    }

    public ExamAttemptStatusResponse getAttemptStatus(String attemptId, String username) {
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = getOwnedAttempt(attemptId, student.getId());
        attempt = autoSubmitIfExpired(attempt, student.getAccountId());
        Exam exam = getExamOrThrow(attempt.getExamId());
        return ExamAttemptStatusResponse.of(
                attempt.getId(),
                attempt.getExamId(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getDeadline(),
                Instant.now(),
                canViewScore(exam),
                attempt.getTotalScore()
        );
    }

    public ExamAttempt autoSubmitIfExpired(ExamAttempt attempt, String userId) {
        if (attempt == null || attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS || !isExpired(attempt, Instant.now())) {
            return attempt;
        }
        Exam exam = getExamOrThrow(attempt.getExamId());
        return autoSubmitExpiredAttempt(attempt, exam, userId);
    }

    private ExamAttempt autoSubmitExpiredAttempt(ExamAttempt attempt, Exam exam, String userId) {
        ExamAttempt savedAttempt = gradeAndCloseAttempt(attempt, exam, ExamAttemptStatus.EXPIRED);
        logAutoSubmit(savedAttempt, userId);
        return savedAttempt;
    }

    private ExamAttempt gradeAndCloseAttempt(ExamAttempt attempt, Exam exam, ExamAttemptStatus finalStatus) {
        List<ExamAttemptQuestionSnapshot> snapshots = attempt.getQuestionSnapshots() == null
                ? List.of()
                : attempt.getQuestionSnapshots()
                        .stream()
                        .sorted(Comparator.comparingInt(ExamAttemptQuestionSnapshot::getOrderIndex))
                        .toList();
        if (snapshots.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam attempt has no question snapshot");
        }

        Map<String, AttemptAnswer> answersByQuestionId = attemptAnswerRepository.findByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity(), (left, right) -> left));
        List<String> questionIds = snapshots.stream()
                .map(ExamAttemptQuestionSnapshot::getQuestionId)
                .filter(Objects::nonNull)
                .toList();
        Map<String, Question> questionsById = questionRepository.findAllById(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        BigDecimal totalScore = BigDecimal.ZERO;
        List<AttemptAnswer> gradedAnswers = new ArrayList<>();
        for (ExamAttemptQuestionSnapshot snapshot : snapshots) {
            Question question = questionsById.get(snapshot.getQuestionId());
            AttemptAnswer answer = answersByQuestionId.getOrDefault(snapshot.getQuestionId(), new AttemptAnswer());
            GradeResult grade = gradeQuestion(snapshot, question, answer.getStudentAnswer());
            answer.setAttemptId(attempt.getId());
            answer.setExamId(attempt.getExamId());
            answer.setQuestionId(snapshot.getQuestionId());
            answer.setScoreAchieved(grade.scoreAchieved());
            answer.setIsCorrect(grade.status() == AttemptAnswerStatus.CORRECT);
            answer.setStatus(grade.status());
            answer.setCorrectAnswerSnapshot(grade.correctAnswerSnapshot());
            gradedAnswers.add(answer);
            totalScore = totalScore.add(grade.scoreAchieved());
        }

        attemptAnswerRepository.saveAll(gradedAnswers);
        attempt.setTotalScore(totalScore);
        attempt.setSubmittedAt(resolveSubmittedAt(attempt, finalStatus));
        attempt.setStatus(finalStatus);
        return examAttemptRepository.save(attempt);
    }

    private Instant resolveSubmittedAt(ExamAttempt attempt, ExamAttemptStatus finalStatus) {
        if (finalStatus == ExamAttemptStatus.EXPIRED && attempt.getDeadline() != null) {
            return attempt.getDeadline().truncatedTo(ChronoUnit.MILLIS);
        }
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private GradeResult gradeQuestion(
            ExamAttemptQuestionSnapshot snapshot,
            Question question,
            AttemptAnswerValue answerValue
    ) {
        BigDecimal fullScore = resolveQuestionScore(snapshot, question);
        if (question == null || question.getAnswerDefinition() == null || snapshot.getType() == null) {
            return wrong(Map.of());
        }
        AnswerDefinition answerDefinition = question.getAnswerDefinition();
        return switch (snapshot.getType()) {
            case TRUE_FALSE -> gradeTrueFalse(fullScore, answerDefinition, answerValue);
            case MULTIPLE_CHOICE -> gradeMultipleChoice(fullScore, answerDefinition, answerValue);
            case FILL_BLANK -> gradeFillBlank(fullScore, answerDefinition, answerValue);
            case MATCHING -> gradeMatching(fullScore, answerDefinition, answerValue);
        };
    }

    private GradeResult gradeTrueFalse(
            BigDecimal fullScore,
            AnswerDefinition answerDefinition,
            AttemptAnswerValue answerValue
    ) {
        Map<String, Object> correctSnapshot = correctSnapshot("trueFalseAnswer", answerDefinition.getTrueFalseAnswer());
        if (answerValue == null || answerValue.getTrueFalseAnswer() == null) {
            return notAnswered(correctSnapshot);
        }
        boolean correct = Objects.equals(answerValue.getTrueFalseAnswer(), answerDefinition.getTrueFalseAnswer());
        return correct ? correct(fullScore, correctSnapshot) : wrong(correctSnapshot);
    }

    private GradeResult gradeMultipleChoice(
            BigDecimal fullScore,
            AnswerDefinition answerDefinition,
            AttemptAnswerValue answerValue
    ) {
        List<String> correctOptionIds = answerDefinition.getOptions() == null
                ? List.of()
                : answerDefinition.getOptions()
                        .stream()
                        .filter(AnswerOption::isCorrect)
                        .map(AnswerOption::getOptionId)
                        .filter(StringUtils::hasText)
                        .toList();
        Map<String, Object> correctSnapshot = correctSnapshot("correctOptionIds", correctOptionIds);
        if (answerValue == null || !StringUtils.hasText(answerValue.getSelectedOptionId())) {
            return notAnswered(correctSnapshot);
        }
        boolean correct = correctOptionIds.contains(answerValue.getSelectedOptionId());
        return correct ? correct(fullScore, correctSnapshot) : wrong(correctSnapshot);
    }

    private GradeResult gradeFillBlank(
            BigDecimal fullScore,
            AnswerDefinition answerDefinition,
            AttemptAnswerValue answerValue
    ) {
        FillBlankAnswer fillBlank = answerDefinition.getFillBlankAnswer();
        Map<String, Object> correctSnapshot = fillBlankSnapshot(fillBlank);
        if (answerValue == null || !StringUtils.hasText(answerValue.getFillBlankText())) {
            return notAnswered(correctSnapshot);
        }
        if (fillBlank == null || fillBlank.getAcceptedAnswers() == null || fillBlank.getAcceptedAnswers().isEmpty()) {
            return wrong(correctSnapshot);
        }

        String submitted = normalizeFillBlank(answerValue.getFillBlankText(), fillBlank);
        boolean correct = fillBlank.getAcceptedAnswers()
                .stream()
                .filter(Objects::nonNull)
                .map(accepted -> normalizeFillBlank(accepted, fillBlank))
                .anyMatch(submitted::equals);
        return correct ? correct(fullScore, correctSnapshot) : wrong(correctSnapshot);
    }

    private GradeResult gradeMatching(
            BigDecimal fullScore,
            AnswerDefinition answerDefinition,
            AttemptAnswerValue answerValue
    ) {
        List<MatchingPair> correctPairs = answerDefinition.getMatchingPairs() == null
                ? List.of()
                : answerDefinition.getMatchingPairs();
        Map<String, Object> correctSnapshot = matchingSnapshot(correctPairs);
        if (answerValue == null || answerValue.getMatchingPairs() == null || answerValue.getMatchingPairs().isEmpty()) {
            return notAnswered(correctSnapshot);
        }

        Map<String, String> correctByLeftId = correctPairs.stream()
                .filter(pair -> StringUtils.hasText(pair.getMatchingId()))
                .collect(Collectors.toMap(MatchingPair::getMatchingId, MatchingPair::getRightText, (left, right) -> left));
        Map<String, String> submittedByLeftId = answerValue.getMatchingPairs()
                .stream()
                .filter(pair -> pair != null && StringUtils.hasText(pair.getLeftId()))
                .collect(Collectors.toMap(
                        AttemptMatchingPairAnswer::getLeftId,
                        AttemptMatchingPairAnswer::getRightText,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        boolean correct = !correctByLeftId.isEmpty()
                && submittedByLeftId.size() == correctByLeftId.size()
                && correctByLeftId.entrySet()
                        .stream()
                        .allMatch(entry -> Objects.equals(entry.getValue(), submittedByLeftId.get(entry.getKey())));
        return correct ? correct(fullScore, correctSnapshot) : wrong(correctSnapshot);
    }

    private GradeResult correct(BigDecimal score, Map<String, Object> correctSnapshot) {
        return new GradeResult(AttemptAnswerStatus.CORRECT, score, correctSnapshot);
    }

    private GradeResult wrong(Map<String, Object> correctSnapshot) {
        return new GradeResult(AttemptAnswerStatus.WRONG, BigDecimal.ZERO, correctSnapshot);
    }

    private GradeResult notAnswered(Map<String, Object> correctSnapshot) {
        return new GradeResult(AttemptAnswerStatus.NOT_ANSWERED, BigDecimal.ZERO, correctSnapshot);
    }

    private BigDecimal resolveQuestionScore(ExamAttemptQuestionSnapshot snapshot, Question question) {
        if (snapshot.getScore() != null) {
            return snapshot.getScore();
        }
        if (question != null && question.getScore() != null) {
            return question.getScore();
        }
        return BigDecimal.ZERO;
    }

    private Map<String, Object> correctSnapshot(String key, Object value) {
        Map<String, Object> snapshot = new HashMap<>();
        if (value != null) {
            snapshot.put(key, value);
        }
        return snapshot;
    }

    private Map<String, Object> fillBlankSnapshot(FillBlankAnswer fillBlank) {
        if (fillBlank == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("acceptedAnswers", fillBlank.getAcceptedAnswers() == null
                ? List.of()
                : new ArrayList<>(fillBlank.getAcceptedAnswers()));
        snapshot.put("ignoreCase", fillBlank.isIgnoreCase());
        snapshot.put("ignoreAccent", fillBlank.isIgnoreAccent());
        snapshot.put("trimSpace", fillBlank.isTrimSpace());
        return snapshot;
    }

    private Map<String, Object> matchingSnapshot(List<MatchingPair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return Map.of();
        }
        List<Map<String, String>> pairSnapshots = pairs.stream()
                .sorted(Comparator.comparingInt(MatchingPair::getOrderIndex))
                .map(pair -> {
                    Map<String, String> pairSnapshot = new LinkedHashMap<>();
                    pairSnapshot.put("matchingId", pair.getMatchingId());
                    pairSnapshot.put("leftText", pair.getLeftText());
                    pairSnapshot.put("rightText", pair.getRightText());
                    return pairSnapshot;
                })
                .toList();
        return Map.of("pairs", pairSnapshots);
    }

    private String normalizeFillBlank(String value, FillBlankAnswer fillBlank) {
        String normalized = value;
        if (fillBlank.isTrimSpace()) {
            normalized = normalized.trim();
        }
        if (fillBlank.isIgnoreAccent()) {
            normalized = normalized.replace('\u0111', 'd').replace('\u0110', 'D');
            normalized = DIACRITIC_PATTERN
                    .matcher(Normalizer.normalize(normalized, Normalizer.Form.NFD))
                    .replaceAll("");
        }
        if (fillBlank.isIgnoreCase()) {
            normalized = normalized.toLowerCase(java.util.Locale.ROOT);
        }
        return normalized;
    }

    private boolean isExpired(ExamAttempt attempt, Instant now) {
        return attempt.getDeadline() != null && !now.isBefore(attempt.getDeadline());
    }

    private boolean canViewScore(Exam exam) {
        boolean allowViewScore = exam.getSettings() != null && exam.getSettings().isShowScoreImmediately();
        return allowViewScore || exam.getResultStatus() == ResultPublishStatus.PUBLISHED;
    }

    private SubmitAttemptResponse toSubmitResponse(ExamAttempt attempt, Exam exam) {
        boolean scoreVisible = canViewScore(exam);
        return new SubmitAttemptResponse(
                true,
                attempt.getId(),
                attempt.getStatus(),
                attempt.getSubmittedAt(),
                scoreVisible,
                scoreVisible ? attempt.getTotalScore() : null
        );
    }

    private void logAutoSubmit(ExamAttempt attempt, String userId) {
        systemLogService.log(
                userId,
                "AUTO_SUBMIT_ATTEMPT",
                "EXAM_ATTEMPT",
                attempt.getId(),
                "System auto-submitted expired attempt, examId=" + attempt.getExamId()
        );
    }

    private ExamAttempt getOwnedAttempt(String attemptId, String studentId) {
        return examAttemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam attempt not found"));
    }

    private Exam getExamOrThrow(String examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
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

    private record GradeResult(
            AttemptAnswerStatus status,
            BigDecimal scoreAchieved,
            Map<String, Object> correctAnswerSnapshot
    ) {
    }
}
