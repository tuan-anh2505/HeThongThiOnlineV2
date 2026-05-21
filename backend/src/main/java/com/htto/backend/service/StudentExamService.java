package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.ExamQuestion;
import com.htto.backend.domain.ExamSession;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import com.htto.backend.dto.response.QuestionStudentResponse;
import com.htto.backend.dto.response.StudentExamDetailResponse;
import com.htto.backend.dto.response.StudentExamListResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamQuestionRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.ExamSessionRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentExamService {

    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final MongoTemplate mongoTemplate;

    public StudentExamService(
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository,
            ClassStudentRepository classStudentRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            ExamRepository examRepository,
            ExamSessionRepository examSessionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamQuestionRepository examQuestionRepository,
            QuestionRepository questionRepository,
            MongoTemplate mongoTemplate
    ) {
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classStudentRepository = classStudentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<StudentExamListResponse> getStudentExams(String username) {
        StudentScope scope = getStudentScope(username);
        if (scope.classIds().isEmpty()) {
            return List.of();
        }

        Query query = new Query();
        query.addCriteria(new Criteria().andOperator(
                Criteria.where("classIds").in(scope.classIds()),
                Criteria.where("status").in(List.of(ExamStatus.PUBLISHED, ExamStatus.OPEN))
        ));

        return mongoTemplate.find(query, Exam.class)
                .stream()
                .map(exam -> toListResponse(exam, scope))
                .filter(Objects::nonNull)
                .toList();
    }

    public StudentExamDetailResponse getStudentExamDetail(String examId, String username) {
        StudentScope scope = getStudentScope(username);
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        ensureExamVisibleToStudent(exam, scope);

        SchoolClass schoolClass = resolveStudentExamClass(exam, scope);
        Subject subject = subjectRepository.findById(exam.getSubjectId()).orElse(null);
        ExamSession session = resolveDisplaySession(exam);
        SubmissionSummary submissionSummary = getSubmissionSummary(exam.getId(), scope.student().getId());

        return new StudentExamDetailResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getSubjectId(),
                subject == null ? null : subject.getSubjectName(),
                schoolClass == null ? null : schoolClass.getId(),
                schoolClass == null ? null : schoolClass.getClassName(),
                exam.getDurationMinutes(),
                exam.getMaxAttempts(),
                exam.getSettings() != null && exam.getSettings().isShowScoreImmediately(),
                exam.getSettings() != null && exam.getSettings().isAllowReview(),
                exam.getSettings() != null && exam.getSettings().isShowCorrectAnswers(),
                exam.getSettings() != null && exam.getSettings().isShuffleQuestions(),
                exam.getSettings() != null && exam.getSettings().isShuffleAnswers(),
                session == null ? null : session.getStartTime(),
                session == null ? null : session.getEndTime(),
                exam.getStatus(),
                session == null ? null : session.getStatus(),
                submissionSummary.attemptStatus(),
                submissionSummary.attemptCount(),
                getStudentQuestions(exam)
        );
    }

    private StudentExamListResponse toListResponse(Exam exam, StudentScope scope) {
        SchoolClass schoolClass = resolveStudentExamClass(exam, scope);
        if (schoolClass == null) {
            return null;
        }

        Subject subject = subjectRepository.findById(exam.getSubjectId()).orElse(null);
        ExamSession session = resolveDisplaySession(exam);
        SubmissionSummary submissionSummary = getSubmissionSummary(exam.getId(), scope.student().getId());

        return new StudentExamListResponse(
                exam.getId(),
                exam.getTitle(),
                subject == null ? null : subject.getSubjectName(),
                schoolClass.getClassName(),
                exam.getDurationMinutes(),
                session == null ? null : session.getStartTime(),
                session == null ? null : session.getEndTime(),
                exam.getStatus(),
                session == null ? null : session.getStatus(),
                submissionSummary.attemptStatus(),
                submissionSummary.attemptCount(),
                exam.getMaxAttempts()
        );
    }

    private List<QuestionStudentResponse> getStudentQuestions(Exam exam) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        if (examQuestions.isEmpty() && (exam.getQuestionRefs() == null || exam.getQuestionRefs().isEmpty())) {
            return List.of();
        }

        List<String> questionIds = examQuestions.isEmpty()
                ? exam.getQuestionRefs()
                        .stream()
                        .sorted(Comparator.comparingInt(ExamQuestionRef::getDisplayOrder))
                        .map(ExamQuestionRef::getQuestionId)
                        .toList()
                : examQuestions.stream().map(ExamQuestion::getQuestionId).toList();

        Map<String, Question> questionsById = questionRepository.findAllById(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        return questionIds.stream()
                .map(questionsById::get)
                .filter(Objects::nonNull)
                .map(QuestionStudentResponse::from)
                .toList();
    }

    private void ensureExamVisibleToStudent(Exam exam, StudentScope scope) {
        if (exam.getStatus() != ExamStatus.PUBLISHED && exam.getStatus() != ExamStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        if (resolveStudentExamClass(exam, scope) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot access exam from another class");
        }
    }

    private SchoolClass resolveStudentExamClass(Exam exam, StudentScope scope) {
        if (exam.getClassIds() == null || exam.getClassIds().isEmpty()) {
            return null;
        }
        return exam.getClassIds()
                .stream()
                .filter(scope.classIds()::contains)
                .map(scope.classesById()::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private ExamSession resolveDisplaySession(Exam exam) {
        return examSessionRepository.findByExamIdOrderByStartTimeAsc(exam.getId())
                .stream()
                .map(this::refreshSessionStatus)
                .filter(session -> session.getStatus() != ExamSessionStatus.CANCELLED)
                .min(Comparator.comparingInt(this::sessionDisplayRank)
                        .thenComparing(ExamSession::getStartTime))
                .orElse(null);
    }

    private int sessionDisplayRank(ExamSession session) {
        return switch (session.getStatus()) {
            case IN_PROGRESS -> 0;
            case NOT_OPEN -> 1;
            case FINISHED -> 2;
            case CANCELLED -> 3;
        };
    }

    private ExamSession refreshSessionStatus(ExamSession session) {
        if (session.getStatus() == ExamSessionStatus.CANCELLED) {
            return session;
        }

        Instant now = Instant.now();
        ExamSessionStatus nextStatus;
        if (now.isBefore(session.getStartTime())) {
            nextStatus = ExamSessionStatus.NOT_OPEN;
        } else if (now.isAfter(session.getEndTime())) {
            nextStatus = ExamSessionStatus.FINISHED;
        } else {
            nextStatus = ExamSessionStatus.IN_PROGRESS;
        }

        if (session.getStatus() != nextStatus) {
            session.setStatus(nextStatus);
            return examSessionRepository.save(session);
        }
        return session;
    }

    private SubmissionSummary getSubmissionSummary(String examId, String studentId) {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentIdOrderByAttemptNumberAsc(
                examId,
                studentId
        );
        if (attempts.isEmpty()) {
            return new SubmissionSummary("NOT_STARTED", 0);
        }

        Instant now = Instant.now();
        attempts.stream()
                .filter(attempt -> attempt.getStatus() == ExamAttemptStatus.IN_PROGRESS)
                .filter(attempt -> attempt.getDeadline() != null && !now.isBefore(attempt.getDeadline()))
                .forEach(attempt -> {
                    attempt.setStatus(ExamAttemptStatus.EXPIRED);
                    examAttemptRepository.save(attempt);
                });

        ExamAttempt latest = attempts.stream()
                .max(Comparator.comparingInt(ExamAttempt::getAttemptNumber))
                .orElse(null);
        return new SubmissionSummary(
                latest == null ? "NOT_STARTED" : latest.getStatus().name(),
                (int) attempts.stream()
                        .filter(attempt -> attempt.getStatus() != ExamAttemptStatus.CANCELLED)
                        .count()
        );
    }

    private StudentScope getStudentScope(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (account.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student role is required");
        }

        StudentProfile student = studentProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));

        Set<String> classIds = classStudentRepository.findByStudentIdAndStatus(
                        student.getId(),
                        EnrollmentStatus.ACTIVE
                )
                .stream()
                .map(ClassStudent::getClassId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, SchoolClass> classesById = schoolClassRepository.findAllById(classIds)
                .stream()
                .collect(Collectors.toMap(SchoolClass::getId, Function.identity()));

        return new StudentScope(student, classIds, classesById);
    }

    private record StudentScope(
            StudentProfile student,
            Set<String> classIds,
            Map<String, SchoolClass> classesById
    ) {
    }

    private record SubmissionSummary(String attemptStatus, int attemptCount) {
    }
}
