package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AttemptAnswer;
import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import com.htto.backend.dto.response.AttemptAnswerValueResponse;
import com.htto.backend.dto.response.ExamAttemptQuestionResponse;
import com.htto.backend.dto.response.ExamResultStatusResponse;
import com.htto.backend.dto.response.TeacherAttemptDetailResponse;
import com.htto.backend.dto.response.TeacherAttemptQuestionResponse;
import com.htto.backend.dto.response.TeacherAttemptSummaryResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AttemptAnswerRepository;
import com.htto.backend.repository.ClassSubjectTeacherRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
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
public class TeacherAttemptService {

    private final AccountRepository accountRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ClassSubjectTeacherRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamAttemptSubmitService examAttemptSubmitService;
    private final SystemLogService systemLogService;

    public TeacherAttemptService(
            AccountRepository accountRepository,
            TeacherProfileRepository teacherProfileRepository,
            StudentProfileRepository studentProfileRepository,
            ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository,
            AttemptAnswerRepository attemptAnswerRepository,
            ClassSubjectTeacherRepository assignmentRepository,
            SubjectRepository subjectRepository,
            ExamAttemptSubmitService examAttemptSubmitService,
            SystemLogService systemLogService
    ) {
        this.accountRepository = accountRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.examAttemptSubmitService = examAttemptSubmitService;
        this.systemLogService = systemLogService;
    }

    public List<TeacherAttemptSummaryResponse> getExamAttempts(String examId, String username) {
        TeacherScope scope = getTeacherScope(username);
        Exam exam = getAccessibleExam(examId, scope.teacher());
        BigDecimal maxScore = resolveMaxScore(exam);
        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(exam.getId())
                .stream()
                .map(attempt -> examAttemptSubmitService.autoSubmitIfExpired(attempt, scope.account().getId()))
                .sorted(Comparator.comparing(ExamAttempt::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(ExamAttempt::getAttemptNumber))
                .toList();

        StudentLookup studentLookup = loadStudentLookup(attempts.stream()
                .map(ExamAttempt::getStudentId)
                .collect(Collectors.toSet()));

        return attempts.stream()
                .map(attempt -> toSummary(attempt, studentLookup, maxScore))
                .toList();
    }

    public TeacherAttemptDetailResponse getAttemptDetail(String attemptId, String username) {
        TeacherScope scope = getTeacherScope(username);
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam attempt not found"));
        Exam exam = getAccessibleExam(attempt.getExamId(), scope.teacher());
        attempt = examAttemptSubmitService.autoSubmitIfExpired(attempt, scope.account().getId());

        StudentLookup studentLookup = loadStudentLookup(Set.of(attempt.getStudentId()));
        StudentInfo studentInfo = studentLookup.byStudentId().get(attempt.getStudentId());
        Subject subject = getSubject(exam);
        Map<String, AttemptAnswer> answersByQuestionId = attemptAnswerRepository.findByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity(), (left, right) -> left));

        List<TeacherAttemptQuestionResponse> questions = getOrderedSnapshots(attempt)
                .stream()
                .map(snapshot -> toQuestionResponse(snapshot, answersByQuestionId.get(snapshot.getQuestionId())))
                .toList();

        return new TeacherAttemptDetailResponse(
                attempt.getId(),
                attempt.getExamId(),
                exam.getTitle(),
                subject == null ? null : subject.getSubjectName(),
                attempt.getStudentId(),
                studentInfo == null ? null : studentInfo.studentCode(),
                studentInfo == null ? null : studentInfo.fullName(),
                studentInfo == null ? null : studentInfo.email(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getStatus(),
                attempt.getTotalScore(),
                resolveMaxScore(exam),
                attempt.getAttemptNumber(),
                questions
        );
    }

    public ExamResultStatusResponse publishResults(String examId, String username) {
        TeacherScope scope = getTeacherScope(username);
        Exam exam = getAccessibleExam(examId, scope.teacher());
        exam.setResultStatus(ResultPublishStatus.PUBLISHED);
        Exam saved = examRepository.save(exam);
        systemLogService.log(scope.account().getId(), "PUBLISH_RESULTS", "EXAM", saved.getId(), "Published exam results");
        return new ExamResultStatusResponse(saved.getId(), saved.getResultStatus());
    }

    public ExamResultStatusResponse hideResults(String examId, String username) {
        TeacherScope scope = getTeacherScope(username);
        Exam exam = getAccessibleExam(examId, scope.teacher());
        exam.setResultStatus(ResultPublishStatus.NOT_PUBLISHED);
        Exam saved = examRepository.save(exam);
        systemLogService.log(scope.account().getId(), "HIDE_RESULTS", "EXAM", saved.getId(), "Hid exam results");
        return new ExamResultStatusResponse(saved.getId(), saved.getResultStatus());
    }

    private TeacherAttemptSummaryResponse toSummary(
            ExamAttempt attempt,
            StudentLookup studentLookup,
            BigDecimal maxScore
    ) {
        StudentInfo studentInfo = studentLookup.byStudentId().get(attempt.getStudentId());
        return new TeacherAttemptSummaryResponse(
                attempt.getId(),
                attempt.getExamId(),
                attempt.getStudentId(),
                studentInfo == null ? null : studentInfo.studentCode(),
                studentInfo == null ? null : studentInfo.fullName(),
                studentInfo == null ? null : studentInfo.email(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getStatus(),
                attempt.getTotalScore(),
                maxScore,
                attempt.getAttemptNumber()
        );
    }

    private TeacherAttemptQuestionResponse toQuestionResponse(
            ExamAttemptQuestionSnapshot snapshot,
            AttemptAnswer answer
    ) {
        return new TeacherAttemptQuestionResponse(
                ExamAttemptQuestionResponse.from(snapshot),
                answer == null ? null : AttemptAnswerValueResponse.from(answer.getStudentAnswer()),
                answer == null ? null : answer.getStatus(),
                answer == null ? null : answer.getIsCorrect(),
                answer == null ? null : answer.getScoreAchieved(),
                answer == null ? null : answer.getCorrectAnswerSnapshot()
        );
    }

    private Exam getAccessibleExam(String examId, TeacherProfile teacher) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        ensureCanAccessExam(teacher, exam);
        return exam;
    }

    private void ensureCanAccessExam(TeacherProfile teacher, Exam exam) {
        if (teacher.getId().equals(exam.getTeacherId())) {
            return;
        }
        boolean assigned = exam.getClassIds() != null && exam.getClassIds()
                .stream()
                .anyMatch(classId -> assignmentRepository.findByClassIdAndSubjectIdAndTeacherIdAndStatus(
                                classId,
                                exam.getSubjectId(),
                                teacher.getId(),
                                AssignmentStatus.ACTIVE
                        )
                        .isPresent());
        if (!assigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher cannot access this exam");
        }
    }

    private StudentLookup loadStudentLookup(Set<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return new StudentLookup(Map.of());
        }

        Map<String, StudentProfile> profilesById = studentProfileRepository.findAllById(studentIds)
                .stream()
                .collect(Collectors.toMap(StudentProfile::getId, Function.identity()));
        Map<String, Account> accountsById = accountRepository.findAllById(profilesById.values()
                        .stream()
                        .map(StudentProfile::getAccountId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        Map<String, StudentInfo> byStudentId = profilesById.values()
                .stream()
                .collect(Collectors.toMap(StudentProfile::getId, profile -> {
                    Account account = accountsById.get(profile.getAccountId());
                    return new StudentInfo(
                            profile.getStudentCode(),
                            account == null ? null : account.getFullName(),
                            account == null ? null : account.getEmail()
                    );
                }));
        return new StudentLookup(byStudentId);
    }

    private BigDecimal resolveMaxScore(Exam exam) {
        if (exam.getTotalScore() != null) {
            return exam.getTotalScore();
        }
        if (exam.getQuestionRefs() == null) {
            return BigDecimal.ZERO;
        }
        return exam.getQuestionRefs()
                .stream()
                .map(ref -> ref.getPoint() == null ? BigDecimal.ZERO : ref.getPoint())
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

    private TeacherScope getTeacherScope(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher role is required");
        }
        TeacherProfile teacher = teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        return new TeacherScope(account, teacher);
    }

    private record TeacherScope(Account account, TeacherProfile teacher) {
    }

    private record StudentLookup(Map<String, StudentInfo> byStudentId) {
    }

    private record StudentInfo(String studentCode, String fullName, String email) {
    }
}
