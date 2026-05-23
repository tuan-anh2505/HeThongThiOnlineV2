package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.ExamQuestion;
import com.htto.backend.domain.ExamSession;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.embedded.AnswerDefinition;
import com.htto.backend.domain.embedded.AnswerOption;
import com.htto.backend.domain.embedded.ExamAttemptFillBlankRuleSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptMatchingLeftSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptMatchingRightSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptMatchingSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptOptionSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import com.htto.backend.domain.embedded.MatchingPair;
import com.htto.backend.dto.request.StartExamRequest;
import com.htto.backend.dto.response.ExamAttemptResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamQuestionRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.ExamSessionRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.StudentProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamAttemptService {

    private final ExamAttemptRepository examAttemptRepository;
    private final ExamRepository examRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AccountRepository accountRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassStudentRepository classStudentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExamAttemptSubmitService examAttemptSubmitService;
    private final SystemLogService systemLogService;

    public ExamAttemptService(
            ExamAttemptRepository examAttemptRepository,
            ExamRepository examRepository,
            ExamSessionRepository examSessionRepository,
            ExamQuestionRepository examQuestionRepository,
            QuestionRepository questionRepository,
            AccountRepository accountRepository,
            StudentProfileRepository studentProfileRepository,
            ClassStudentRepository classStudentRepository,
            PasswordEncoder passwordEncoder,
            ExamAttemptSubmitService examAttemptSubmitService,
            SystemLogService systemLogService
    ) {
        this.examAttemptRepository = examAttemptRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
        this.accountRepository = accountRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.classStudentRepository = classStudentRepository;
        this.passwordEncoder = passwordEncoder;
        this.examAttemptSubmitService = examAttemptSubmitService;
        this.systemLogService = systemLogService;
    }

    public ExamAttemptResponse startExam(String examId, StartExamRequest request, String username) {
        StudentProfile student = getCurrentStudent(username);
        Exam exam = getExamOrThrow(examId);
        ensureExamCanBeStarted(exam, student);
        ExamSession session = getActiveSessionOrThrow(exam.getId());
        Instant now = Instant.now();
        verifyExamPassword(exam, request == null ? null : request.examPassword(), student);

        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdAndStudentIdOrderByAttemptNumberAsc(
                exam.getId(),
                student.getId()
        );
        for (ExamAttempt attempt : attempts) {
            ExamAttempt refreshed = examAttemptSubmitService.autoSubmitIfExpired(attempt, student.getAccountId());
            if (refreshed.getStatus() == ExamAttemptStatus.IN_PROGRESS) {
                return ExamAttemptResponse.from(refreshed, exam, now);
            }
        }

        long countedAttempts = attempts.stream()
                .filter(attempt -> attempt.getStatus() != ExamAttemptStatus.CANCELLED)
                .count();
        if (countedAttempts >= exam.getMaxAttempts()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student has reached max attempts");
        }

        int nextAttemptNumber = attempts.stream()
                .mapToInt(ExamAttempt::getAttemptNumber)
                .max()
                .orElse(0) + 1;
        Instant deadline = calculateDeadline(now, session, exam);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setStudentId(student.getId());
        attempt.setExamId(exam.getId());
        attempt.setSessionId(session.getId());
        attempt.setStartedAt(now.truncatedTo(ChronoUnit.MILLIS));
        attempt.setDeadline(deadline);
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setQuestionSnapshots(buildQuestionSnapshots(exam));

        ExamAttempt saved = examAttemptRepository.save(attempt);
        systemLogService.log(student.getAccountId(), "START_EXAM", "EXAM_ATTEMPT", saved.getId(), "Student started exam");
        return ExamAttemptResponse.from(saved, exam, now);
    }

    public ExamAttemptResponse getAttempt(String attemptId, String username) {
        StudentProfile student = getCurrentStudent(username);
        ExamAttempt attempt = examAttemptRepository.findByIdAndStudentId(attemptId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam attempt not found"));
        Instant now = Instant.now();
        ExamAttempt refreshed = examAttemptSubmitService.autoSubmitIfExpired(attempt, student.getAccountId());
        Exam exam = examRepository.findById(refreshed.getExamId()).orElse(null);
        return ExamAttemptResponse.from(refreshed, exam, now, canViewScore(exam));
    }

    private void ensureExamCanBeStarted(Exam exam, StudentProfile student) {
        if (exam.getStatus() != ExamStatus.PUBLISHED && exam.getStatus() != ExamStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam is not published or open");
        }
        if (!isStudentAssignedToExamClass(student.getId(), exam.getClassIds())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot access exam from another class");
        }
    }

    private void verifyExamPassword(Exam exam, String examPassword, StudentProfile student) {
        if (!Boolean.TRUE.equals(exam.getHasPassword())) {
            return;
        }
        if (!StringUtils.hasText(examPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài thi yêu cầu mật khẩu");
        }
        if (!StringUtils.hasText(exam.getExamPasswordHash())
                || !passwordEncoder.matches(examPassword, exam.getExamPasswordHash())) {
            logWrongExamPassword(exam, student);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu bài thi không đúng");
        }
    }

    private void logWrongExamPassword(Exam exam, StudentProfile student) {
        systemLogService.log(
                student.getAccountId(),
                "WRONG_EXAM_PASSWORD",
                "EXAM",
                exam.getId(),
                "Student entered wrong exam password, examId=" + exam.getId()
        );
    }

    private boolean canViewScore(Exam exam) {
        if (exam == null) {
            return false;
        }
        boolean allowViewScore = exam.getSettings() != null && exam.getSettings().isShowScoreImmediately();
        return allowViewScore || exam.getResultStatus() == ResultPublishStatus.PUBLISHED;
    }

    private ExamSession getActiveSessionOrThrow(String examId) {
        List<ExamSession> sessions = examSessionRepository.findByExamIdOrderByStartTimeAsc(examId)
                .stream()
                .map(this::refreshSessionStatus)
                .toList();
        Instant now = Instant.now();

        return sessions.stream()
                .filter(session -> session.getStatus() != ExamSessionStatus.CANCELLED)
                .filter(session -> !now.isBefore(session.getStartTime()) && !now.isAfter(session.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        resolveNoActiveSessionMessage(sessions, now)
                ));
    }

    private String resolveNoActiveSessionMessage(List<ExamSession> sessions, Instant now) {
        if (sessions.isEmpty()) {
            return "Exam has no session";
        }
        boolean hasFutureSession = sessions.stream()
                .filter(session -> session.getStatus() != ExamSessionStatus.CANCELLED)
                .anyMatch(session -> now.isBefore(session.getStartTime()));
        if (hasFutureSession) {
            return "Exam session has not started";
        }
        return "Exam session has finished";
    }

    private Instant calculateDeadline(Instant startedAt, ExamSession session, Exam exam) {
        Instant durationDeadline = startedAt.plus(Math.max(0, exam.getDurationMinutes()), ChronoUnit.MINUTES);
        if (exam.getDurationMinutes() <= 0) {
            return session.getEndTime();
        }
        return durationDeadline.isBefore(session.getEndTime()) ? durationDeadline : session.getEndTime();
    }

    private List<ExamAttemptQuestionSnapshot> buildQuestionSnapshots(Exam exam) {
        List<String> questionIds = getExamQuestionIds(exam);
        if (questionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam has no questions");
        }

        Map<String, Question> questionById = questionRepository.findAllById(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<Question> questions = questionIds.stream()
                .map(questionById::get)
                .filter(Objects::nonNull)
                .toList();
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam has no valid questions");
        }

        List<Question> orderedQuestions = new ArrayList<>(questions);
        if (exam.getSettings() != null && exam.getSettings().isShuffleQuestions()) {
            Collections.shuffle(orderedQuestions);
        }

        List<ExamAttemptQuestionSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < orderedQuestions.size(); i++) {
            snapshots.add(toQuestionSnapshot(
                    orderedQuestions.get(i),
                    i + 1,
                    exam.getSettings() != null && exam.getSettings().isShuffleAnswers()
            ));
        }
        return snapshots;
    }

    private List<String> getExamQuestionIds(Exam exam) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        if (!examQuestions.isEmpty()) {
            return examQuestions.stream().map(ExamQuestion::getQuestionId).toList();
        }
        if (exam.getQuestionRefs() == null || exam.getQuestionRefs().isEmpty()) {
            return List.of();
        }
        return exam.getQuestionRefs()
                .stream()
                .sorted(Comparator.comparingInt(ExamQuestionRef::getDisplayOrder))
                .map(ExamQuestionRef::getQuestionId)
                .toList();
    }

    private ExamAttemptQuestionSnapshot toQuestionSnapshot(Question question, int orderIndex, boolean shuffleOptions) {
        ExamAttemptQuestionSnapshot snapshot = new ExamAttemptQuestionSnapshot();
        snapshot.setQuestionId(question.getId());
        snapshot.setType(question.getType());
        snapshot.setContent(question.getContent());
        snapshot.setScore(question.getScore());
        snapshot.setDifficulty(question.getDifficulty());
        snapshot.setTopic(question.getTopic());
        snapshot.setOrderIndex(orderIndex);

        AnswerDefinition answer = question.getAnswerDefinition();
        if (answer == null) {
            return snapshot;
        }
        snapshot.setOptions(toOptionSnapshots(
                answer.getOptions(),
                shuffleOptions && question.getType() == QuestionType.MULTIPLE_CHOICE
        ));
        snapshot.setFillBlankRule(toFillBlankRuleSnapshot(answer));
        snapshot.setMatching(toMatchingSnapshot(answer.getMatchingPairs()));
        return snapshot;
    }

    private List<ExamAttemptOptionSnapshot> toOptionSnapshots(List<AnswerOption> options, boolean shuffleOptions) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<AnswerOption> orderedOptions = options.stream()
                .sorted(Comparator.comparingInt(AnswerOption::getOrderIndex))
                .collect(Collectors.toCollection(ArrayList::new));
        if (shuffleOptions) {
            Collections.shuffle(orderedOptions);
        }

        List<ExamAttemptOptionSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < orderedOptions.size(); i++) {
            AnswerOption option = orderedOptions.get(i);
            ExamAttemptOptionSnapshot snapshot = new ExamAttemptOptionSnapshot();
            snapshot.setOptionId(option.getOptionId());
            snapshot.setContent(option.getContent());
            snapshot.setOrderIndex(i + 1);
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private ExamAttemptFillBlankRuleSnapshot toFillBlankRuleSnapshot(AnswerDefinition answer) {
        if (answer.getFillBlankAnswer() == null) {
            return null;
        }
        ExamAttemptFillBlankRuleSnapshot snapshot = new ExamAttemptFillBlankRuleSnapshot();
        snapshot.setIgnoreCase(answer.getFillBlankAnswer().isIgnoreCase());
        snapshot.setIgnoreAccent(answer.getFillBlankAnswer().isIgnoreAccent());
        snapshot.setTrimSpace(answer.getFillBlankAnswer().isTrimSpace());
        return snapshot;
    }

    private ExamAttemptMatchingSnapshot toMatchingSnapshot(List<MatchingPair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return null;
        }

        ExamAttemptMatchingSnapshot snapshot = new ExamAttemptMatchingSnapshot();
        snapshot.setLeftItems(pairs.stream()
                .sorted(Comparator.comparingInt(MatchingPair::getOrderIndex))
                .map(pair -> {
                    ExamAttemptMatchingLeftSnapshot item = new ExamAttemptMatchingLeftSnapshot();
                    item.setMatchingId(pair.getMatchingId());
                    item.setLeftText(pair.getLeftText());
                    item.setOrderIndex(pair.getOrderIndex());
                    return item;
                })
                .toList());
        List<String> rightTexts = pairs.stream()
                .map(MatchingPair::getRightText)
                .sorted()
                .toList();
        List<ExamAttemptMatchingRightSnapshot> rightItems = new ArrayList<>();
        for (int i = 0; i < rightTexts.size(); i++) {
            ExamAttemptMatchingRightSnapshot item = new ExamAttemptMatchingRightSnapshot();
            item.setRightText(rightTexts.get(i));
            item.setOrderIndex(i + 1);
            rightItems.add(item);
        }
        snapshot.setRightItems(rightItems);
        return snapshot;
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
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return examClassIds.stream().anyMatch(activeClassIds::contains);
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

    private Exam getExamOrThrow(String id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }
}
