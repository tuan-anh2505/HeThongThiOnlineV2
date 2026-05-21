package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.AcademicStatus;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamSession;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.dto.request.ExamSessionCreateRequest;
import com.htto.backend.dto.request.ExamSessionUpdateRequest;
import com.htto.backend.dto.response.ExamSessionAccessResponse;
import com.htto.backend.dto.response.ExamSessionResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.ExamSessionRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final AccountRepository accountRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SystemLogService systemLogService;

    public ExamSessionService(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            AccountRepository accountRepository,
            TeacherProfileRepository teacherProfileRepository,
            StudentProfileRepository studentProfileRepository,
            ClassStudentRepository classStudentRepository,
            SystemLogService systemLogService
    ) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.accountRepository = accountRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classStudentRepository = classStudentRepository;
        this.systemLogService = systemLogService;
    }

    public List<ExamSessionResponse> getExamSessions(String examId, String username) {
        Exam exam = getExamOrThrow(examId);
        ensureCanManageExam(getCurrentAccount(username), exam);

        return examSessionRepository.findByExamIdOrderByStartTimeAsc(exam.getId())
                .stream()
                .map(this::refreshStatus)
                .map(ExamSessionResponse::from)
                .toList();
    }

    public ExamSessionResponse createSession(
            String examId,
            ExamSessionCreateRequest request,
            String username
    ) {
        validateCreateRequest(request);
        Exam exam = getExamOrThrow(examId);
        ensureCanManageExam(getCurrentAccount(username), exam);
        validateTimeRange(request.startTime(), request.endTime());

        ExamSession examSession = new ExamSession();
        examSession.setExamId(exam.getId());
        examSession.setStartTime(request.startTime());
        examSession.setEndTime(request.endTime());
        examSession.setStatus(resolveTemporalStatus(request.startTime(), request.endTime(), Instant.now()));

        ExamSession saved = examSessionRepository.save(examSession);
        systemLogService.logCurrentUser("CREATE_EXAM_SESSION", "EXAM_SESSION", saved.getId(), "Created exam session");
        return ExamSessionResponse.from(saved);
    }

    public ExamSessionResponse getSession(String id, String username) {
        ExamSession examSession = refreshStatus(getSessionOrThrow(id));
        Exam exam = getExamOrThrow(examSession.getExamId());
        ensureCanManageExam(getCurrentAccount(username), exam);
        return ExamSessionResponse.from(examSession);
    }

    public ExamSessionResponse updateSession(
            String id,
            ExamSessionUpdateRequest request,
            String username
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam session payload is required");
        }

        ExamSession examSession = getSessionOrThrow(id);
        Exam exam = getExamOrThrow(examSession.getExamId());
        ensureCanManageExam(getCurrentAccount(username), exam);
        if (examSession.getStatus() == ExamSessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled exam session cannot be updated");
        }

        Instant nextStartTime = request.startTime() == null ? examSession.getStartTime() : request.startTime();
        Instant nextEndTime = request.endTime() == null ? examSession.getEndTime() : request.endTime();
        validateTimeRange(nextStartTime, nextEndTime);

        examSession.setStartTime(nextStartTime);
        examSession.setEndTime(nextEndTime);
        examSession.setStatus(resolveTemporalStatus(nextStartTime, nextEndTime, Instant.now()));
        ExamSession saved = examSessionRepository.save(examSession);
        systemLogService.logCurrentUser("UPDATE_EXAM_SESSION", "EXAM_SESSION", saved.getId(), "Updated exam session");
        return ExamSessionResponse.from(saved);
    }

    public void deleteSession(String id, String username) {
        ExamSession examSession = getSessionOrThrow(id);
        Exam exam = getExamOrThrow(examSession.getExamId());
        ensureCanManageExam(getCurrentAccount(username), exam);

        examSession.setStatus(ExamSessionStatus.CANCELLED);
        examSessionRepository.save(examSession);
        systemLogService.logCurrentUser("CANCEL_EXAM_SESSION", "EXAM_SESSION", examSession.getId(), "Cancelled exam session");
    }

    public ExamSessionResponse cancelSession(String id, String username) {
        ExamSession examSession = getSessionOrThrow(id);
        Exam exam = getExamOrThrow(examSession.getExamId());
        ensureCanManageExam(getCurrentAccount(username), exam);

        examSession.setStatus(ExamSessionStatus.CANCELLED);
        ExamSession saved = examSessionRepository.save(examSession);
        systemLogService.logCurrentUser("CANCEL_EXAM_SESSION", "EXAM_SESSION", saved.getId(), "Cancelled exam session");
        return ExamSessionResponse.from(saved);
    }

    public ExamSessionAccessResponse checkStudentAccess(String id, String username) {
        Account account = getCurrentAccount(username);
        if (account.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student role is required");
        }

        StudentProfile student = studentProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
        if (student.getStatus() != AcademicStatus.ACTIVE) {
            return denied(id, null, null, "Student profile is not active", 0);
        }

        ExamSession examSession = refreshStatus(getSessionOrThrow(id));
        Exam exam = getExamOrThrow(examSession.getExamId());
        Instant now = Instant.now();

        if (exam.getStatus() != ExamStatus.PUBLISHED && exam.getStatus() != ExamStatus.OPEN) {
            return denied(examSession, "Exam is not published or open");
        }
        if (examSession.getStatus() == ExamSessionStatus.CANCELLED) {
            return denied(examSession, "Exam session is cancelled");
        }
        if (now.isBefore(examSession.getStartTime())) {
            return denied(examSession, "Exam session has not started");
        }
        if (now.isAfter(examSession.getEndTime())) {
            return denied(examSession, "Exam session has finished");
        }
        if (!isStudentAssignedToExamClass(student.getId(), exam.getClassIds())) {
            return denied(examSession, "Student is not assigned to exam class");
        }

        long remainingBySession = Math.max(0, Duration.between(now, examSession.getEndTime()).getSeconds());
        long examDurationSeconds = Math.max(0, exam.getDurationMinutes()) * 60L;
        long remainingSeconds = examDurationSeconds == 0
                ? remainingBySession
                : Math.min(examDurationSeconds, remainingBySession);

        return new ExamSessionAccessResponse(
                true,
                null,
                examSession.getId(),
                examSession.getExamId(),
                examSession.getStartTime(),
                examSession.getEndTime(),
                examSession.getStatus(),
                remainingSeconds
        );
    }

    private void validateCreateRequest(ExamSessionCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam session payload is required");
        }
        if (request.startTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime is required");
        }
        if (request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime is required");
        }
    }

    private void validateTimeRange(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime are required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }
    }

    private ExamSession refreshStatus(ExamSession examSession) {
        if (examSession.getStatus() == ExamSessionStatus.CANCELLED) {
            return examSession;
        }

        ExamSessionStatus nextStatus = resolveTemporalStatus(
                examSession.getStartTime(),
                examSession.getEndTime(),
                Instant.now()
        );
        if (examSession.getStatus() != nextStatus) {
            examSession.setStatus(nextStatus);
            return examSessionRepository.save(examSession);
        }
        return examSession;
    }

    private ExamSessionStatus resolveTemporalStatus(Instant startTime, Instant endTime, Instant now) {
        if (now.isBefore(startTime)) {
            return ExamSessionStatus.NOT_OPEN;
        }
        if (now.isAfter(endTime)) {
            return ExamSessionStatus.FINISHED;
        }
        return ExamSessionStatus.IN_PROGRESS;
    }

    private boolean isStudentAssignedToExamClass(String studentId, List<String> examClassIds) {
        if (examClassIds == null || examClassIds.isEmpty()) {
            return false;
        }
        Set<String> activeClassIds = classStudentRepository.findByStudentIdAndStatus(
                        studentId,
                        EnrollmentStatus.ACTIVE
                )
                .stream()
                .map(ClassStudent::getClassId)
                .collect(Collectors.toSet());
        return examClassIds.stream().anyMatch(activeClassIds::contains);
    }

    private ExamSessionAccessResponse denied(ExamSession examSession, String reason) {
        return denied(
                examSession.getId(),
                examSession.getExamId(),
                examSession,
                reason,
                0
        );
    }

    private ExamSessionAccessResponse denied(
            String examSessionId,
            String examId,
            ExamSession examSession,
            String reason,
            long remainingSeconds
    ) {
        return new ExamSessionAccessResponse(
                false,
                reason,
                examSessionId,
                examId,
                examSession == null ? null : examSession.getStartTime(),
                examSession == null ? null : examSession.getEndTime(),
                examSession == null ? null : examSession.getStatus(),
                remainingSeconds
        );
    }

    private void ensureCanManageExam(Account account, Exam exam) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot manage exam sessions");
        }
        TeacherProfile teacher = teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        if (!teacher.getId().equals(exam.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only manage own exam sessions");
        }
    }

    private Exam getExamOrThrow(String id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private ExamSession getSessionOrThrow(String id) {
        return examSessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam session not found"));
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
