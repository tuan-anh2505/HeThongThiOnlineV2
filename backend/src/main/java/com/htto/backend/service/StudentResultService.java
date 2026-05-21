package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AttemptAnswer;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import com.htto.backend.dto.response.AttemptAnswerValueResponse;
import com.htto.backend.dto.response.ExamAttemptQuestionResponse;
import com.htto.backend.dto.response.StudentAttemptResultResponse;
import com.htto.backend.dto.response.StudentAttemptReviewQuestionResponse;
import com.htto.backend.dto.response.StudentAttemptReviewResponse;
import com.htto.backend.dto.response.StudentResultSummaryResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AttemptAnswerRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentResultService {

    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ExamRepository examRepository;
    private final SubjectRepository subjectRepository;
    private final ExamAttemptSubmitService examAttemptSubmitService;

    public StudentResultService(
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository,
            ExamAttemptRepository examAttemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            ExamRepository examRepository,
            SubjectRepository subjectRepository,
            ExamAttemptSubmitService examAttemptSubmitService
    ) {
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.examRepository = examRepository;
        this.subjectRepository = subjectRepository;
        this.examAttemptSubmitService = examAttemptSubmitService;
    }

    public StudentAttemptResultResponse getAttemptResult(String attemptId, String username) {
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = getFinishedOwnedAttempt(attemptId, student);
        Exam exam = getExamOrThrow(attempt.getExamId());
        Subject subject = getSubject(exam);
        boolean scoreVisible = canViewScore(exam);
        return new StudentAttemptResultResponse(
                attempt.getId(),
                attempt.getExamId(),
                exam.getTitle(),
                subject == null ? null : subject.getSubjectName(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                scoreVisible,
                scoreVisible ? attempt.getTotalScore() : null,
                resolveMaxScore(exam, attempt),
                canReview(exam),
                canViewCorrectAnswer(exam)
        );
    }

    public StudentAttemptReviewResponse getAttemptReview(String attemptId, String username) {
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = getFinishedOwnedAttempt(attemptId, student);
        Exam exam = getExamOrThrow(attempt.getExamId());
        if (!canReview(exam)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Exam review is not allowed");
        }

        Subject subject = getSubject(exam);
        boolean scoreVisible = canViewScore(exam);
        boolean correctAnswerVisible = canViewCorrectAnswer(exam);
        Map<String, AttemptAnswer> answersByQuestionId = attemptAnswerRepository.findByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity(), (left, right) -> left));

        List<StudentAttemptReviewQuestionResponse> questions = getOrderedSnapshots(attempt)
                .stream()
                .map(snapshot -> toReviewQuestion(
                        snapshot,
                        answersByQuestionId.get(snapshot.getQuestionId()),
                        scoreVisible,
                        correctAnswerVisible
                ))
                .toList();

        return new StudentAttemptReviewResponse(
                attempt.getId(),
                attempt.getExamId(),
                exam.getTitle(),
                subject == null ? null : subject.getSubjectName(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                scoreVisible,
                scoreVisible ? attempt.getTotalScore() : null,
                resolveMaxScore(exam, attempt),
                correctAnswerVisible,
                questions
        );
    }

    public List<StudentResultSummaryResponse> getMyResults(String username) {
        StudentProfile student = getCurrentStudent(username);
        List<ExamAttempt> attempts = examAttemptRepository.findByStudentId(student.getId())
                .stream()
                .map(attempt -> examAttemptSubmitService.autoSubmitIfExpired(attempt, student.getAccountId()))
                .filter(this::isFinished)
                .sorted(Comparator.comparing(
                        ExamAttempt::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
        if (attempts.isEmpty()) {
            return List.of();
        }

        Set<String> examIds = attempts.stream().map(ExamAttempt::getExamId).collect(Collectors.toSet());
        Map<String, Exam> examsById = examRepository.findAllById(examIds)
                .stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        Set<String> subjectIds = examsById.values()
                .stream()
                .map(Exam::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Subject> subjectsById = subjectRepository.findAllById(subjectIds)
                .stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));

        return attempts.stream()
                .map(attempt -> toResultSummary(attempt, examsById.get(attempt.getExamId()), subjectsById))
                .filter(Objects::nonNull)
                .toList();
    }

    private StudentResultSummaryResponse toResultSummary(
            ExamAttempt attempt,
            Exam exam,
            Map<String, Subject> subjectsById
    ) {
        if (exam == null) {
            return null;
        }
        Subject subject = subjectsById.get(exam.getSubjectId());
        boolean scoreVisible = canViewScore(exam);
        return new StudentResultSummaryResponse(
                attempt.getId(),
                attempt.getExamId(),
                exam.getTitle(),
                subject == null ? null : subject.getSubjectName(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getSubmittedAt(),
                scoreVisible,
                scoreVisible ? attempt.getTotalScore() : null,
                resolveMaxScore(exam, attempt),
                canReview(exam)
        );
    }

    private StudentAttemptReviewQuestionResponse toReviewQuestion(
            ExamAttemptQuestionSnapshot snapshot,
            AttemptAnswer answer,
            boolean scoreVisible,
            boolean correctAnswerVisible
    ) {
        return new StudentAttemptReviewQuestionResponse(
                snapshot.getQuestionId(),
                snapshot.getType(),
                snapshot.getContent(),
                snapshot.getScore(),
                snapshot.getDifficulty(),
                snapshot.getTopic(),
                snapshot.getOrderIndex(),
                ExamAttemptQuestionResponse.from(snapshot),
                answer == null ? null : AttemptAnswerValueResponse.from(answer.getStudentAnswer()),
                answer == null ? null : answer.getStatus(),
                answer == null ? null : answer.getIsCorrect(),
                scoreVisible && answer != null ? answer.getScoreAchieved() : null,
                correctAnswerVisible && answer != null ? answer.getCorrectAnswerSnapshot() : null
        );
    }

    private ExamAttempt getFinishedOwnedAttempt(String attemptId, StudentProfile student) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam attempt not found"));
        attempt = examAttemptSubmitService.autoSubmitIfExpired(attempt, student.getAccountId());
        if (!isFinished(attempt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam attempt is not finished");
        }
        return attempt;
    }

    private boolean isFinished(ExamAttempt attempt) {
        return attempt.getStatus() == ExamAttemptStatus.SUBMITTED || attempt.getStatus() == ExamAttemptStatus.EXPIRED;
    }

    private boolean canViewScore(Exam exam) {
        boolean allowViewScore = exam.getSettings() != null && exam.getSettings().isShowScoreImmediately();
        return allowViewScore || exam.getResultStatus() == ResultPublishStatus.PUBLISHED;
    }

    private boolean canReview(Exam exam) {
        return exam.getSettings() != null && exam.getSettings().isAllowReview();
    }

    private boolean canViewCorrectAnswer(Exam exam) {
        return exam.getSettings() != null && exam.getSettings().isShowCorrectAnswers();
    }

    private BigDecimal resolveMaxScore(Exam exam, ExamAttempt attempt) {
        if (exam.getTotalScore() != null) {
            return exam.getTotalScore();
        }
        return getOrderedSnapshots(attempt)
                .stream()
                .map(ExamAttemptQuestionSnapshot::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ExamAttemptQuestionSnapshot> getOrderedSnapshots(ExamAttempt attempt) {
        if (attempt.getQuestionSnapshots() == null) {
            return List.of();
        }
        return attempt.getQuestionSnapshots()
                .stream()
                .sorted(Comparator.comparingInt(ExamAttemptQuestionSnapshot::getOrderIndex))
                .toList();
    }

    private Subject getSubject(Exam exam) {
        if (exam.getSubjectId() == null) {
            return null;
        }
        return subjectRepository.findById(exam.getSubjectId()).orElse(null);
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
}
