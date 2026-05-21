package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.dto.response.ClassExamStatisticsResponse;
import com.htto.backend.dto.response.ClassStatisticsResponse;
import com.htto.backend.dto.response.ClassStudentExamScoreResponse;
import com.htto.backend.dto.response.ClassStudentStatisticsResponse;
import com.htto.backend.dto.response.ExamStatisticsResponse;
import com.htto.backend.dto.response.ExamStudentScoreStatisticResponse;
import com.htto.backend.dto.response.ScoreChartBucketResponse;
import com.htto.backend.dto.response.StatisticsAttemptScoreResponse;
import com.htto.backend.dto.response.SubjectExamStatisticsResponse;
import com.htto.backend.dto.response.SubjectStatisticsResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ClassSubjectTeacherRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
public class StatisticsService {

    private final AccountRepository accountRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassSubjectTeacherRepository assignmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptSubmitService examAttemptSubmitService;
    private final MongoTemplate mongoTemplate;

    public StatisticsService(
            AccountRepository accountRepository,
            TeacherProfileRepository teacherProfileRepository,
            StudentProfileRepository studentProfileRepository,
            ClassStudentRepository classStudentRepository,
            ClassSubjectTeacherRepository assignmentRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            ExamRepository examRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamAttemptSubmitService examAttemptSubmitService,
            MongoTemplate mongoTemplate
    ) {
        this.accountRepository = accountRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classStudentRepository = classStudentRepository;
        this.assignmentRepository = assignmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.examRepository = examRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examAttemptSubmitService = examAttemptSubmitService;
        this.mongoTemplate = mongoTemplate;
    }

    public ExamStatisticsResponse getExamStatistics(String examId, String username) {
        UserScope scope = getUserScope(username);
        Exam exam = getExamOrThrow(examId);
        ensureCanAccessExam(scope, exam);

        Subject subject = getSubject(exam);
        BigDecimal maxScore = resolveMaxScore(exam);
        Map<String, StudentInfo> assignedStudents = getAssignedStudents(exam.getClassIds());
        Map<String, List<ExamAttempt>> attemptsByStudent = getExamAttempts(exam.getId(), scope.account().getId())
                .stream()
                .collect(Collectors.groupingBy(ExamAttempt::getStudentId, LinkedHashMap::new, Collectors.toList()));

        List<ExamStudentScoreStatisticResponse> studentScores = assignedStudents.values()
                .stream()
                .sorted(Comparator.comparing(StudentInfo::studentCode, Comparator.nullsLast(String::compareTo)))
                .map(student -> toExamStudentScore(student, attemptsByStudent.getOrDefault(student.studentId(), List.of())))
                .toList();

        List<BigDecimal> bestScores = studentScores.stream()
                .map(ExamStudentScoreStatisticResponse::bestScore)
                .filter(Objects::nonNull)
                .toList();
        int attemptedStudentCount = (int) attemptsByStudent.keySet()
                .stream()
                .filter(assignedStudents::containsKey)
                .count();

        return new ExamStatisticsResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getSubjectId(),
                subject == null ? null : subject.getSubjectName(),
                maxScore,
                assignedStudents.size(),
                attemptedStudentCount,
                Math.max(0, assignedStudents.size() - attemptedStudentCount),
                countAttempts(attemptsByStudent.values(), ExamAttemptStatus.SUBMITTED),
                countAttempts(attemptsByStudent.values(), ExamAttemptStatus.EXPIRED),
                max(bestScores),
                min(bestScores),
                average(bestScores),
                studentScores
        );
    }

    public ClassStatisticsResponse getClassStatistics(String classId, String username) {
        UserScope scope = getUserScope(username);
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
        ensureCanAccessClass(scope, schoolClass);

        Map<String, StudentInfo> students = getClassStudents(classId);
        List<Exam> exams = findClassExams(classId)
                .stream()
                .filter(exam -> canAccessExam(scope, exam))
                .sorted(Comparator.comparing(Exam::getTitle, Comparator.nullsLast(String::compareTo)))
                .toList();
        Map<String, Subject> subjectsById = getSubjectsById(exams.stream().map(Exam::getSubjectId).toList());

        Map<String, List<ExamAttempt>> attemptsByExamStudent = getAttemptsForExams(exams, scope.account().getId())
                .stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getExamId() + "|" + attempt.getStudentId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<BigDecimal> allBestScores = new ArrayList<>();
        List<ScoreWithMax> chartScores = new ArrayList<>();
        List<ClassStudentStatisticsResponse> studentRows = students.values()
                .stream()
                .sorted(Comparator.comparing(StudentInfo::studentCode, Comparator.nullsLast(String::compareTo)))
                .map(student -> {
                    List<ClassStudentExamScoreResponse> examScores = exams.stream()
                            .map(exam -> {
                                List<ExamAttempt> attempts = attemptsByExamStudent.getOrDefault(
                                        exam.getId() + "|" + student.studentId(),
                                        List.of()
                                );
                                ScoreSummary scoreSummary = summarizeAttempts(attempts);
                                if (scoreSummary.bestScore() != null) {
                                    allBestScores.add(scoreSummary.bestScore());
                                    chartScores.add(new ScoreWithMax(scoreSummary.bestScore(), resolveMaxScore(exam)));
                                }
                                Subject subject = subjectsById.get(exam.getSubjectId());
                                return new ClassStudentExamScoreResponse(
                                        exam.getId(),
                                        exam.getTitle(),
                                        exam.getSubjectId(),
                                        subject == null ? null : subject.getSubjectName(),
                                        scoreSummary.latestStatus(),
                                        attempts.size(),
                                        scoreSummary.latestScore(),
                                        scoreSummary.bestScore()
                                );
                            })
                            .toList();
                    return new ClassStudentStatisticsResponse(
                            student.studentId(),
                            student.studentCode(),
                            student.fullName(),
                            student.email(),
                            average(examScores.stream()
                                    .map(ClassStudentExamScoreResponse::bestScore)
                                    .filter(Objects::nonNull)
                                    .toList()),
                            examScores
                    );
                })
                .toList();

        List<ClassExamStatisticsResponse> examRows = exams.stream()
                .map(exam -> toClassExamStatistics(exam, subjectsById.get(exam.getSubjectId()), students.keySet(), attemptsByExamStudent))
                .toList();

        return new ClassStatisticsResponse(
                schoolClass.getId(),
                schoolClass.getClassCode(),
                schoolClass.getClassName(),
                students.size(),
                exams.size(),
                average(allBestScores),
                studentRows,
                examRows,
                buildScoreChart(chartScores)
        );
    }

    public SubjectStatisticsResponse getSubjectStatistics(String subjectId, String username) {
        UserScope scope = getUserScope(username);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        ensureCanAccessSubject(scope, subjectId);

        List<Exam> exams = examRepository.findBySubjectId(subjectId)
                .stream()
                .filter(exam -> canAccessExam(scope, exam))
                .sorted(Comparator.comparing(Exam::getTitle, Comparator.nullsLast(String::compareTo)))
                .toList();

        List<SubjectExamStatisticsResponse> examRows = exams.stream()
                .map(exam -> {
                    Map<String, StudentInfo> assignedStudents = getAssignedStudents(exam.getClassIds());
                    Map<String, List<ExamAttempt>> attemptsByStudent = getExamAttempts(exam.getId(), scope.account().getId())
                            .stream()
                            .collect(Collectors.groupingBy(ExamAttempt::getStudentId));
                    List<BigDecimal> bestScores = assignedStudents.keySet()
                            .stream()
                            .map(studentId -> summarizeAttempts(attemptsByStudent.getOrDefault(studentId, List.of())).bestScore())
                            .filter(Objects::nonNull)
                            .toList();
                    int attemptedStudentCount = (int) attemptsByStudent.keySet()
                            .stream()
                            .filter(assignedStudents::containsKey)
                            .count();
                    return new SubjectExamStatisticsResponse(
                            exam.getId(),
                            exam.getTitle(),
                            assignedStudents.size(),
                            attemptedStudentCount,
                            average(bestScores)
                    );
                })
                .toList();

        return new SubjectStatisticsResponse(
                subject.getId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                exams.size(),
                examRows
        );
    }

    private ClassExamStatisticsResponse toClassExamStatistics(
            Exam exam,
            Subject subject,
            Set<String> studentIds,
            Map<String, List<ExamAttempt>> attemptsByExamStudent
    ) {
        List<BigDecimal> bestScores = studentIds.stream()
                .map(studentId -> summarizeAttempts(attemptsByExamStudent.getOrDefault(exam.getId() + "|" + studentId, List.of()))
                        .bestScore())
                .filter(Objects::nonNull)
                .toList();
        int attemptedStudentCount = (int) studentIds.stream()
                .filter(studentId -> attemptsByExamStudent.containsKey(exam.getId() + "|" + studentId))
                .count();
        return new ClassExamStatisticsResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getSubjectId(),
                subject == null ? null : subject.getSubjectName(),
                studentIds.size(),
                attemptedStudentCount,
                average(bestScores)
        );
    }

    private ExamStudentScoreStatisticResponse toExamStudentScore(StudentInfo student, List<ExamAttempt> attempts) {
        List<ExamAttempt> orderedAttempts = attempts.stream()
                .sorted(Comparator.comparingInt(ExamAttempt::getAttemptNumber))
                .toList();
        ScoreSummary summary = summarizeAttempts(orderedAttempts);
        return new ExamStudentScoreStatisticResponse(
                student.studentId(),
                student.studentCode(),
                student.fullName(),
                student.email(),
                orderedAttempts.size(),
                (int) orderedAttempts.stream().filter(attempt -> attempt.getStatus() == ExamAttemptStatus.SUBMITTED).count(),
                (int) orderedAttempts.stream().filter(attempt -> attempt.getStatus() == ExamAttemptStatus.EXPIRED).count(),
                summary.latestStatus(),
                summary.latestSubmittedAt(),
                summary.latestScore(),
                summary.bestScore(),
                orderedAttempts.stream()
                        .map(attempt -> new StatisticsAttemptScoreResponse(
                                attempt.getId(),
                                attempt.getAttemptNumber(),
                                attempt.getStatus(),
                                attempt.getStartedAt(),
                                attempt.getSubmittedAt(),
                                attempt.getTotalScore()
                        ))
                        .toList()
        );
    }

    private ScoreSummary summarizeAttempts(List<ExamAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return new ScoreSummary(null, null, null, null);
        }
        List<ExamAttempt> orderedAttempts = attempts.stream()
                .sorted(Comparator.comparingInt(ExamAttempt::getAttemptNumber))
                .toList();
        ExamAttempt latest = orderedAttempts.get(orderedAttempts.size() - 1);
        BigDecimal bestScore = orderedAttempts.stream()
                .map(ExamAttempt::getTotalScore)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
        return new ScoreSummary(
                latest.getStatus(),
                latest.getSubmittedAt(),
                latest.getTotalScore(),
                bestScore
        );
    }

    private List<ExamAttempt> getExamAttempts(String examId, String userId) {
        return examAttemptRepository.findByExamId(examId)
                .stream()
                .map(attempt -> examAttemptSubmitService.autoSubmitIfExpired(attempt, userId))
                .toList();
    }

    private List<ExamAttempt> getAttemptsForExams(List<Exam> exams, String userId) {
        if (exams.isEmpty()) {
            return List.of();
        }
        Set<String> examIds = exams.stream().map(Exam::getId).collect(Collectors.toSet());
        return examIds.stream()
                .flatMap(examId -> getExamAttempts(examId, userId).stream())
                .toList();
    }

    private Map<String, StudentInfo> getAssignedStudents(List<String> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Map.of();
        }
        Set<String> studentIds = classIds.stream()
                .flatMap(classId -> classStudentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE).stream())
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return loadStudents(studentIds);
    }

    private Map<String, StudentInfo> getClassStudents(String classId) {
        Set<String> studentIds = classStudentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return loadStudents(studentIds);
    }

    private Map<String, StudentInfo> loadStudents(Collection<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
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
        return profilesById.values()
                .stream()
                .collect(Collectors.toMap(
                        StudentProfile::getId,
                        profile -> {
                            Account account = accountsById.get(profile.getAccountId());
                            return new StudentInfo(
                                    profile.getId(),
                                    profile.getStudentCode(),
                                    account == null ? null : account.getFullName(),
                                    account == null ? null : account.getEmail()
                            );
                        },
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<Exam> findClassExams(String classId) {
        Query query = new Query(Criteria.where("classIds").is(classId));
        return mongoTemplate.find(query, Exam.class);
    }

    private Map<String, Subject> getSubjectsById(Collection<String> subjectIds) {
        Set<String> ids = subjectIds == null
                ? Set.of()
                : subjectIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity()));
    }

    private List<ScoreChartBucketResponse> buildScoreChart(List<ScoreWithMax> scores) {
        int[] counts = new int[4];
        for (ScoreWithMax score : scores) {
            if (score.maxScore() == null || score.maxScore().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal percent = score.score()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(score.maxScore(), 4, RoundingMode.HALF_UP);
            if (percent.compareTo(BigDecimal.valueOf(50)) < 0) {
                counts[0]++;
            } else if (percent.compareTo(BigDecimal.valueOf(65)) < 0) {
                counts[1]++;
            } else if (percent.compareTo(BigDecimal.valueOf(80)) < 0) {
                counts[2]++;
            } else {
                counts[3]++;
            }
        }
        return List.of(
                new ScoreChartBucketResponse("0-49%", 0, 49, counts[0]),
                new ScoreChartBucketResponse("50-64%", 50, 64, counts[1]),
                new ScoreChartBucketResponse("65-79%", 65, 79, counts[2]),
                new ScoreChartBucketResponse("80-100%", 80, 100, counts[3])
        );
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

    private int countAttempts(Collection<List<ExamAttempt>> groupedAttempts, ExamAttemptStatus status) {
        return (int) groupedAttempts.stream()
                .flatMap(List::stream)
                .filter(attempt -> attempt.getStatus() == status)
                .count();
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNullValues = values == null
                ? List.of()
                : values.stream().filter(Objects::nonNull).toList();
        if (nonNullValues.isEmpty()) {
            return null;
        }
        BigDecimal sum = nonNullValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNullValues.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(List<BigDecimal> values) {
        return values.stream().max(BigDecimal::compareTo).orElse(null);
    }

    private BigDecimal min(List<BigDecimal> values) {
        return values.stream().min(BigDecimal::compareTo).orElse(null);
    }

    private void ensureCanAccessExam(UserScope scope, Exam exam) {
        if (!canAccessExam(scope, exam)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to view exam statistics");
        }
    }

    private boolean canAccessExam(UserScope scope, Exam exam) {
        if (scope.account().getRole() == Role.ADMIN) {
            return true;
        }
        TeacherProfile teacher = scope.teacher();
        if (teacher == null) {
            return false;
        }
        if (teacher.getId().equals(exam.getTeacherId())) {
            return true;
        }
        if (exam.getClassIds() == null || exam.getClassIds().isEmpty()) {
            return false;
        }
        return exam.getClassIds()
                .stream()
                .anyMatch(classId -> assignmentRepository.findByClassIdAndSubjectIdAndTeacherIdAndStatus(
                                classId,
                                exam.getSubjectId(),
                                teacher.getId(),
                                AssignmentStatus.ACTIVE
                        )
                        .isPresent());
    }

    private void ensureCanAccessClass(UserScope scope, SchoolClass schoolClass) {
        if (scope.account().getRole() == Role.ADMIN) {
            return;
        }
        TeacherProfile teacher = scope.teacher();
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to view class statistics");
        }
        boolean directTeacher = teacher.getId().equals(schoolClass.getTeacherId());
        boolean assigned = assignmentRepository.findByTeacherIdAndStatus(teacher.getId(), AssignmentStatus.ACTIVE)
                .stream()
                .map(ClassSubjectTeacher::getClassId)
                .anyMatch(schoolClass.getId()::equals);
        if (!directTeacher && !assigned) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to view class statistics");
        }
    }

    private void ensureCanAccessSubject(UserScope scope, String subjectId) {
        if (scope.account().getRole() == Role.ADMIN) {
            return;
        }
        TeacherProfile teacher = scope.teacher();
        if (teacher == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to view subject statistics");
        }
        boolean assigned = assignmentRepository.findByTeacherIdAndStatus(teacher.getId(), AssignmentStatus.ACTIVE)
                .stream()
                .map(ClassSubjectTeacher::getSubjectId)
                .anyMatch(subjectId::equals);
        boolean hasOwnExam = examRepository.findBySubjectId(subjectId)
                .stream()
                .anyMatch(exam -> teacher.getId().equals(exam.getTeacherId()));
        if (!assigned && !hasOwnExam) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to view subject statistics");
        }
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

    private UserScope getUserScope(String username) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (account.getRole() == Role.ADMIN) {
            return new UserScope(account, null);
        }
        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = teacherProfileRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
            return new UserScope(account, teacher);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot access statistics");
    }

    private record UserScope(Account account, TeacherProfile teacher) {
    }

    private record StudentInfo(String studentId, String studentCode, String fullName, String email) {
    }

    private record ScoreSummary(
            ExamAttemptStatus latestStatus,
            java.time.Instant latestSubmittedAt,
            BigDecimal latestScore,
            BigDecimal bestScore
    ) {
    }

    private record ScoreWithMax(BigDecimal score, BigDecimal maxScore) {
    }
}
