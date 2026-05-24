package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.DomainEnums.ProfileStatus;
import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.domain.DomainEnums.SelectionMode;
import com.htto.backend.domain.DomainEnums.SubjectStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamQuestion;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.QuestionBank;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import com.htto.backend.domain.embedded.ExamSettings;
import com.htto.backend.domain.embedded.QuestionSelectionConfig;
import com.htto.backend.dto.request.ExamCreateRequest;
import com.htto.backend.dto.request.ExamQuestionCreateRequest;
import com.htto.backend.dto.request.ExamUpdateRequest;
import com.htto.backend.dto.request.GenerateRandomQuestionsRequest;
import com.htto.backend.dto.request.RandomQuestionConfigRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.dto.response.ClassResponse;
import com.htto.backend.dto.response.ExamQuestionResponse;
import com.htto.backend.dto.response.ExamResponse;
import com.htto.backend.dto.response.QuestionBankResponse;
import com.htto.backend.dto.response.QuestionResponse;
import com.htto.backend.dto.response.SubjectResponse;
import com.htto.backend.dto.response.TeacherProfileResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.ClassSubjectTeacherRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamQuestionRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.QuestionBankRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassSubjectTeacherRepository assignmentRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AccountRepository accountRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;
    private final SystemLogService systemLogService;
    private final NotificationService notificationService;

    public ExamService(
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            SubjectRepository subjectRepository,
            SchoolClassRepository schoolClassRepository,
            ClassSubjectTeacherRepository assignmentRepository,
            TeacherProfileRepository teacherProfileRepository,
            AccountRepository accountRepository,
            ExamAttemptRepository examAttemptRepository,
            PasswordEncoder passwordEncoder,
            MongoTemplate mongoTemplate,
            SystemLogService systemLogService,
            NotificationService notificationService
    ) {
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.subjectRepository = subjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.assignmentRepository = assignmentRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.accountRepository = accountRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
        this.systemLogService = systemLogService;
        this.notificationService = notificationService;
    }

    public List<ExamResponse> searchExams(
            String examName,
            String classId,
            String subjectId,
            String questionBankId,
            String teacherId,
            ExamStatus status,
            ResultPublishStatus resultStatus,
            String username
    ) {
        Account account = getCurrentAccount(username);
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        addRegexCriteria(criteria, "title", examName);
        addExactCriteria(criteria, "classIds", classId);
        addExactCriteria(criteria, "subjectId", subjectId);
        addExactCriteria(criteria, "questionBankId", questionBankId);
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        if (resultStatus != null) {
            criteria.add(Criteria.where("resultStatus").is(resultStatus));
        }

        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = getTeacherProfile(account);
            criteria.add(Criteria.where("teacherId").is(teacher.getId()));
        } else if (account.getRole() == Role.ADMIN) {
            addExactCriteria(criteria, "teacherId", teacherId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot manage exams");
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        }

        return mongoTemplate.find(query, Exam.class)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExamResponse createExam(ExamCreateRequest request, String username) {
        Account account = getCurrentAccount(username);
        validateCreatePayload(request);

        Subject subject = getActiveSubject(request.subjectId());
        QuestionBank questionBank = getActiveQuestionBank(request.questionBankId());
        ensureQuestionBankMatchesSubject(questionBank, subject.getId());

        TeacherProfile teacher = resolveTeacherForCreate(account, request.teacherId(), questionBank);
        List<String> classIds = normalizeClassIds(request.classId(), request.classIds());
        if (classIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one class is required");
        }
        validateClassAssignments(classIds, subject.getId(), teacher.getId());
        ensureExamNameAvailable(request.examName(), teacher.getId(), null);

        Exam exam = new Exam();
        exam.setTitle(request.examName().trim());
        exam.setClassIds(classIds);
        exam.setSubjectId(subject.getId());
        exam.setQuestionBankId(questionBank.getId());
        exam.setTeacherId(teacher.getId());
        exam.setDurationMinutes(validatePositive(request.durationMinutes(), "durationMinutes"));
        exam.setMaxAttempts(validatePositiveOrDefault(request.maxAttempts(), 1, "maxAttempts"));
        exam.setTotalScore(BigDecimal.ZERO);
        exam.setSettings(toSettings(
                request.allowViewScore(),
                request.allowReview(),
                request.allowViewCorrectAnswer(),
                request.shuffleQuestions(),
                request.shuffleOptions()
        ));
        exam.setSelectionConfig(newSelectionConfig(0, BigDecimal.ZERO));
        applyCreatePassword(exam, request.examPassword());
        exam.setResultStatus(ResultPublishStatus.NOT_PUBLISHED);
        exam.setStatus(ExamStatus.DRAFT);

        Exam saved = examRepository.save(exam);
        systemLogService.logCurrentUser("CREATE_EXAM", "EXAM", saved.getId(), "Created exam");
        if (Boolean.TRUE.equals(saved.getHasPassword())) {
            logExamPasswordAction(account, saved, "CREATE_EXAM_PASSWORD", "Created exam with password");
        }
        return toResponse(saved);
    }

    public ExamResponse getExam(String id, String username) {
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        return toResponse(exam);
    }

    public ExamResponse updateExam(String id, ExamUpdateRequest request, String username) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam payload is required");
        }
        Exam exam = getExamOrThrow(id);
        Account account = getCurrentAccount(username);
        ensureCanManageExam(account, exam);
        ensureNoSubmissions(exam.getId());
        ensureEditableStatus(exam);

        String nextTitle = StringUtils.hasText(request.examName()) ? request.examName().trim() : exam.getTitle();
        String nextSubjectId = StringUtils.hasText(request.subjectId()) ? request.subjectId().trim() : exam.getSubjectId();
        String nextQuestionBankId = StringUtils.hasText(request.questionBankId())
                ? request.questionBankId().trim()
                : exam.getQuestionBankId();
        List<String> nextClassIds = hasClassUpdate(request)
                ? normalizeClassIds(request.classId(), request.classIds())
                : exam.getClassIds();

        if (!StringUtils.hasText(nextTitle)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "examName is required");
        }
        if (nextClassIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one class is required");
        }

        Subject subject = getActiveSubject(nextSubjectId);
        QuestionBank questionBank = getActiveQuestionBank(nextQuestionBankId);
        ensureQuestionBankMatchesSubject(questionBank, subject.getId());
        TeacherProfile teacher = resolveTeacherForUpdate(account, request.teacherId(), exam, questionBank);

        boolean questionBankChanged = !questionBank.getId().equals(exam.getQuestionBankId());
        if (questionBankChanged && !loadOrMigrateExamQuestions(exam).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot change question bank while exam has questions"
            );
        }

        validateClassAssignments(nextClassIds, subject.getId(), teacher.getId());
        ensureExamNameAvailable(nextTitle, teacher.getId(), exam.getId());

        exam.setTitle(nextTitle);
        exam.setClassIds(nextClassIds);
        exam.setSubjectId(subject.getId());
        exam.setQuestionBankId(questionBank.getId());
        exam.setTeacherId(teacher.getId());
        if (request.durationMinutes() != null) {
            exam.setDurationMinutes(validatePositive(request.durationMinutes(), "durationMinutes"));
        }
        if (request.maxAttempts() != null) {
            exam.setMaxAttempts(validatePositive(request.maxAttempts(), "maxAttempts"));
        }
        exam.setSettings(applySettings(exam.getSettings(), request));
        PasswordChange passwordChange = applyUpdatePassword(exam, request);

        Exam saved = examRepository.save(exam);
        systemLogService.logCurrentUser("UPDATE_EXAM", "EXAM", saved.getId(), "Updated exam");
        if (passwordChange == PasswordChange.UPDATED) {
            logExamPasswordAction(account, saved, "UPDATE_EXAM_PASSWORD", "Updated exam password");
        } else if (passwordChange == PasswordChange.REMOVED) {
            logExamPasswordAction(account, saved, "REMOVE_EXAM_PASSWORD", "Removed exam password");
        }
        return toResponse(saved);
    }

    public void deleteExam(String id, String username) {
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        exam.setStatus(ExamStatus.CANCELLED);
        examRepository.save(exam);
        systemLogService.logCurrentUser("CANCEL_EXAM", "EXAM", exam.getId(), "Cancelled exam by legacy delete endpoint");
    }

    public ExamResponse addQuestion(String id, ExamQuestionCreateRequest request, String username) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam question payload is required");
        }
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        ensureNoSubmissions(exam.getId());
        ensureQuestionMutableStatus(exam);

        Question question = getActiveQuestion(request.questionId());
        if (!exam.getQuestionBankId().equals(question.getQuestionBankId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to exam question bank");
        }

        List<ExamQuestion> existingQuestions = loadOrMigrateExamQuestions(exam);
        if (existingQuestions.stream().anyMatch(item -> item.getQuestionId().equals(question.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question already exists in exam");
        }

        BigDecimal score = request.score() == null ? question.getScore() : request.score();
        if (score == null || score.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question score must be greater than 0");
        }

        int orderIndex = resolveInsertOrder(request.orderIndex(), existingQuestions.size());
        shiftOrderIndexes(existingQuestions, orderIndex);

        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExamId(exam.getId());
        examQuestion.setQuestionId(question.getId());
        examQuestion.setScore(score);
        examQuestion.setOrderIndex(orderIndex);
        examQuestionRepository.save(examQuestion);

        return toResponse(syncExamQuestions(exam));
    }

    public void removeQuestion(String id, String questionId, String username) {
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        ensureNoSubmissions(exam.getId());
        ensureQuestionMutableStatus(exam);

        loadOrMigrateExamQuestions(exam);
        ExamQuestion examQuestion = examQuestionRepository.findByExamIdAndQuestionId(id, questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam question not found"));
        examQuestionRepository.delete(examQuestion);
        syncExamQuestions(exam);
    }

    public ExamResponse generateRandomQuestions(
            String id,
            GenerateRandomQuestionsRequest request,
            String username
    ) {
        validateRandomRequest(request);
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        ensureNoSubmissions(exam.getId());
        ensureQuestionMutableStatus(exam);

        QuestionBank questionBank = getActiveQuestionBank(exam.getQuestionBankId());
        if (!questionBank.getTeacherId().equals(exam.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam question bank does not belong to teacher");
        }

        List<Question> selectedQuestions = new ArrayList<>();
        Set<String> selectedQuestionIds = new LinkedHashSet<>();
        Map<QuestionType, Integer> quantityByType = new HashMap<>();
        Map<Difficulty, Integer> quantityByDifficulty = new HashMap<>();

        for (int i = 0; i < request.configs().size(); i++) {
            RandomQuestionConfigRequest config = request.configs().get(i);
            int quantity = validateRandomQuantity(config, i + 1);
            List<Question> candidates = findRandomQuestionCandidates(exam, config, selectedQuestionIds);
            if (candidates.size() < quantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Not enough questions for random config #" + (i + 1)
                                + ". Required " + quantity
                                + ", available " + candidates.size()
                                + ", filters: " + describeRandomConfig(config)
                );
            }

            Collections.shuffle(candidates);
            List<Question> pickedQuestions = candidates.stream()
                    .limit(quantity)
                    .toList();
            pickedQuestions.forEach(question -> {
                selectedQuestions.add(question);
                selectedQuestionIds.add(question.getId());
            });

            if (config.type() != null) {
                quantityByType.merge(config.type(), quantity, Integer::sum);
            }
            if (config.difficulty() != null) {
                quantityByDifficulty.merge(config.difficulty(), quantity, Integer::sum);
            }
        }

        List<ExamQuestion> existingQuestions = loadOrMigrateExamQuestions(exam);
        if (!existingQuestions.isEmpty()) {
            examQuestionRepository.deleteAll(existingQuestions);
        }

        List<ExamQuestion> examQuestions = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question question = selectedQuestions.get(i);
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExamId(exam.getId());
            examQuestion.setQuestionId(question.getId());
            examQuestion.setScore(question.getScore());
            examQuestion.setOrderIndex(i + 1);
            examQuestions.add(examQuestion);
        }
        examQuestionRepository.saveAll(examQuestions);

        return toResponse(syncExamQuestions(
                exam,
                SelectionMode.RANDOM,
                quantityByType,
                quantityByDifficulty
        ));
    }

    public ExamResponse publishExam(String id, String username) {
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        if (exam.getStatus() == ExamStatus.CANCELLED || exam.getStatus() == ExamStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot publish closed or cancelled exam");
        }
        if (loadOrMigrateExamQuestions(exam).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot publish exam without questions");
        }
        exam.setStatus(ExamStatus.PUBLISHED);
        Exam saved = syncExamQuestions(exam);
        systemLogService.logCurrentUser("PUBLISH_EXAM", "EXAM", saved.getId(), "Published exam");
        notificationService.notifyExamPublished(saved);
        return toResponse(saved);
    }

    public ExamResponse closeExam(String id, String username) {
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        if (exam.getStatus() == ExamStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled exam cannot be closed");
        }
        exam.setStatus(ExamStatus.CLOSED);
        Exam saved = examRepository.save(exam);
        systemLogService.logCurrentUser("CLOSE_EXAM", "EXAM", saved.getId(), "Closed exam");
        return toResponse(saved);
    }

    public ExamResponse cancelExam(String id, String username) {
        Exam exam = getExamOrThrow(id);
        ensureCanManageExam(getCurrentAccount(username), exam);
        exam.setStatus(ExamStatus.CANCELLED);
        Exam saved = examRepository.save(exam);
        systemLogService.logCurrentUser("CANCEL_EXAM", "EXAM", saved.getId(), "Cancelled exam");
        return toResponse(saved);
    }

    private void applyCreatePassword(Exam exam, String examPassword) {
        if (StringUtils.hasText(examPassword)) {
            exam.setHasPassword(true);
            exam.setExamPasswordHash(passwordEncoder.encode(examPassword));
            return;
        }

        exam.setHasPassword(false);
        exam.setExamPasswordHash(null);
    }

    private PasswordChange applyUpdatePassword(Exam exam, ExamUpdateRequest request) {
        if (StringUtils.hasText(request.examPassword())) {
            exam.setHasPassword(true);
            exam.setExamPasswordHash(passwordEncoder.encode(request.examPassword()));
            return PasswordChange.UPDATED;
        }

        if (Boolean.TRUE.equals(request.removePassword())) {
            exam.setHasPassword(false);
            exam.setExamPasswordHash(null);
            return PasswordChange.REMOVED;
        }

        return PasswordChange.NONE;
    }

    private void logExamPasswordAction(Account account, Exam exam, String action, String detail) {
        systemLogService.log(account.getId(), action, "EXAM", exam.getId(), detail + ", examId=" + exam.getId());
    }

    private void validateRandomRequest(GenerateRandomQuestionsRequest request) {
        if (request == null || request.configs() == null || request.configs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Random question configs are required");
        }
    }

    private int validateRandomQuantity(RandomQuestionConfigRequest config, int configIndex) {
        if (config == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Random question config #" + configIndex + " is required"
            );
        }
        if (config.quantity() == null || config.quantity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Random question config #" + configIndex + " quantity must be greater than 0"
            );
        }
        return config.quantity();
    }

    private List<Question> findRandomQuestionCandidates(
            Exam exam,
            RandomQuestionConfigRequest config,
            Set<String> excludedQuestionIds
    ) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("questionBankId").is(exam.getQuestionBankId()));
        criteria.add(Criteria.where("status").is(QuestionStatus.ACTIVE));
        if (config.type() != null) {
            criteria.add(Criteria.where("type").is(config.type()));
        }
        if (config.difficulty() != null) {
            criteria.add(Criteria.where("difficulty").is(config.difficulty()));
        }
        if (StringUtils.hasText(config.topic())) {
            Pattern topicPattern = Pattern.compile(
                    "^" + Pattern.quote(config.topic().trim()) + "$",
                    Pattern.CASE_INSENSITIVE
            );
            criteria.add(Criteria.where("topic").regex(topicPattern));
        }
        if (!excludedQuestionIds.isEmpty()) {
            criteria.add(Criteria.where("_id").nin(excludedQuestionIds));
        }
        query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        return mongoTemplate.find(query, Question.class);
    }

    private String describeRandomConfig(RandomQuestionConfigRequest config) {
        List<String> parts = new ArrayList<>();
        if (config.type() != null) {
            parts.add("type=" + config.type());
        }
        if (config.difficulty() != null) {
            parts.add("difficulty=" + config.difficulty());
        }
        if (StringUtils.hasText(config.topic())) {
            parts.add("topic=" + config.topic().trim());
        }
        if (parts.isEmpty()) {
            return "all active questions";
        }
        return String.join(", ", parts);
    }

    private void validateCreatePayload(ExamCreateRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam payload is required");
        }
        if (!StringUtils.hasText(request.examName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "examName is required");
        }
        if (!StringUtils.hasText(request.subjectId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectId is required");
        }
        if (!StringUtils.hasText(request.questionBankId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionBankId is required");
        }
    }

    private TeacherProfile resolveTeacherForCreate(
            Account account,
            String requestedTeacherId,
            QuestionBank questionBank
    ) {
        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = getTeacherProfile(account);
            if (!teacher.getId().equals(questionBank.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only use own question bank");
            }
            if (StringUtils.hasText(requestedTeacherId) && !teacher.getId().equals(requestedTeacherId.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher cannot create exam for another teacher");
            }
            return validateActiveTeacher(teacher.getId());
        }

        if (account.getRole() == Role.ADMIN) {
            String teacherId = StringUtils.hasText(requestedTeacherId)
                    ? requestedTeacherId.trim()
                    : questionBank.getTeacherId();
            if (!teacherId.equals(questionBank.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher must own the question bank");
            }
            return validateActiveTeacher(teacherId);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot manage exams");
    }

    private TeacherProfile resolveTeacherForUpdate(
            Account account,
            String requestedTeacherId,
            Exam exam,
            QuestionBank questionBank
    ) {
        if (account.getRole() == Role.TEACHER) {
            TeacherProfile teacher = getTeacherProfile(account);
            if (StringUtils.hasText(requestedTeacherId) && !teacher.getId().equals(requestedTeacherId.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher cannot reassign exam");
            }
            if (!teacher.getId().equals(questionBank.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only use own question bank");
            }
            return validateActiveTeacher(teacher.getId());
        }

        if (account.getRole() == Role.ADMIN) {
            String teacherId = StringUtils.hasText(requestedTeacherId)
                    ? requestedTeacherId.trim()
                    : questionBank.getTeacherId();
            if (!teacherId.equals(questionBank.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher must own the question bank");
            }
            return validateActiveTeacher(teacherId);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot manage exams");
    }

    private void validateClassAssignments(List<String> classIds, String subjectId, String teacherId) {
        for (String classId : classIds) {
            SchoolClass schoolClass = schoolClassRepository.findById(classId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found: " + classId));
            if (schoolClass.getStatus() != ClassStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class is inactive: " + classId);
            }

            assignmentRepository.findByClassIdAndSubjectIdAndTeacherIdAndStatus(
                            classId,
                            subjectId,
                            teacherId,
                            AssignmentStatus.ACTIVE
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Teacher is not assigned to class and subject: " + classId
                    ));
        }
    }

    private List<String> normalizeClassIds(String classId, List<String> classIds) {
        Set<String> result = new LinkedHashSet<>();
        if (StringUtils.hasText(classId)) {
            result.add(classId.trim());
        }
        if (classIds != null) {
            classIds.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(result::add);
        }
        return new ArrayList<>(result);
    }

    private boolean hasClassUpdate(ExamUpdateRequest request) {
        return request.classId() != null || request.classIds() != null;
    }

    private int validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be greater than 0");
        }
        return value;
    }

    private int validatePositiveOrDefault(Integer value, int defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        return validatePositive(value, fieldName);
    }

    private int resolveInsertOrder(Integer requestedOrder, int currentSize) {
        if (requestedOrder == null) {
            return currentSize + 1;
        }
        if (requestedOrder <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderIndex must be greater than 0");
        }
        return Math.min(requestedOrder, currentSize + 1);
    }

    private void shiftOrderIndexes(List<ExamQuestion> existingQuestions, int orderIndex) {
        List<ExamQuestion> changed = existingQuestions.stream()
                .filter(item -> item.getOrderIndex() >= orderIndex)
                .peek(item -> item.setOrderIndex(item.getOrderIndex() + 1))
                .toList();
        if (!changed.isEmpty()) {
            examQuestionRepository.saveAll(changed);
        }
    }

    private ExamSettings toSettings(
            Boolean allowViewScore,
            Boolean allowReview,
            Boolean allowViewCorrectAnswer,
            Boolean shuffleQuestions,
            Boolean shuffleOptions
    ) {
        ExamSettings settings = new ExamSettings();
        settings.setShowScoreImmediately(Boolean.TRUE.equals(allowViewScore));
        settings.setAllowReview(Boolean.TRUE.equals(allowReview));
        settings.setShowCorrectAnswers(Boolean.TRUE.equals(allowViewCorrectAnswer));
        settings.setShuffleQuestions(Boolean.TRUE.equals(shuffleQuestions));
        settings.setShuffleAnswers(Boolean.TRUE.equals(shuffleOptions));
        return settings;
    }

    private ExamSettings applySettings(ExamSettings settings, ExamUpdateRequest request) {
        if (settings == null) {
            settings = new ExamSettings();
        }
        if (request.allowViewScore() != null) {
            settings.setShowScoreImmediately(request.allowViewScore());
        }
        if (request.allowReview() != null) {
            settings.setAllowReview(request.allowReview());
        }
        if (request.allowViewCorrectAnswer() != null) {
            settings.setShowCorrectAnswers(request.allowViewCorrectAnswer());
        }
        if (request.shuffleQuestions() != null) {
            settings.setShuffleQuestions(request.shuffleQuestions());
        }
        if (request.shuffleOptions() != null) {
            settings.setShuffleAnswers(request.shuffleOptions());
        }
        return settings;
    }

    private QuestionSelectionConfig newSelectionConfig(int totalQuestions, BigDecimal totalScore) {
        return newSelectionConfig(
                SelectionMode.MANUAL,
                totalQuestions,
                totalScore,
                Map.of(),
                Map.of()
        );
    }

    private QuestionSelectionConfig newSelectionConfig(
            SelectionMode selectionMode,
            int totalQuestions,
            BigDecimal totalScore,
            Map<QuestionType, Integer> quantityByType,
            Map<Difficulty, Integer> quantityByDifficulty
    ) {
        QuestionSelectionConfig selectionConfig = new QuestionSelectionConfig();
        selectionConfig.setSelectionMode(selectionMode);
        selectionConfig.setTotalQuestions(totalQuestions);
        selectionConfig.setTotalScore(totalScore);
        selectionConfig.setQuantityByType(new HashMap<>(quantityByType));
        selectionConfig.setQuantityByDifficulty(new HashMap<>(quantityByDifficulty));
        return selectionConfig;
    }

    private List<ExamQuestion> loadOrMigrateExamQuestions(Exam exam) {
        List<ExamQuestion> existing = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        if (exam.getQuestionRefs() == null || exam.getQuestionRefs().isEmpty()) {
            return List.of();
        }

        Map<String, ExamQuestion> migratedByQuestionId = new LinkedHashMap<>();
        for (int i = 0; i < exam.getQuestionRefs().size(); i++) {
            ExamQuestionRef ref = exam.getQuestionRefs().get(i);
            if (ref == null || !StringUtils.hasText(ref.getQuestionId())) {
                continue;
            }
            int displayOrder = ref.getDisplayOrder() > 0 ? ref.getDisplayOrder() : i + 1;
            migratedByQuestionId.computeIfAbsent(ref.getQuestionId(), ignored -> {
                ExamQuestion examQuestion = new ExamQuestion();
                examQuestion.setExamId(exam.getId());
                examQuestion.setQuestionId(ref.getQuestionId());
                examQuestion.setScore(ref.getPoint());
                examQuestion.setOrderIndex(displayOrder);
                return examQuestion;
            });
        }
        if (migratedByQuestionId.isEmpty()) {
            return List.of();
        }
        return examQuestionRepository.saveAll(migratedByQuestionId.values())
                .stream()
                .sorted(Comparator.comparingInt(ExamQuestion::getOrderIndex))
                .toList();
    }

    private Exam syncExamQuestions(Exam exam) {
        return syncExamQuestions(exam, SelectionMode.MANUAL, Map.of(), Map.of());
    }

    private Exam syncExamQuestions(
            Exam exam,
            SelectionMode selectionMode,
            Map<QuestionType, Integer> quantityByType,
            Map<Difficulty, Integer> quantityByDifficulty
    ) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        List<ExamQuestion> normalized = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        List<ExamQuestionRef> refs = new ArrayList<>();

        for (int i = 0; i < examQuestions.size(); i++) {
            ExamQuestion examQuestion = examQuestions.get(i);
            int nextOrder = i + 1;
            if (examQuestion.getOrderIndex() != nextOrder) {
                examQuestion.setOrderIndex(nextOrder);
                normalized.add(examQuestion);
            }

            BigDecimal score = examQuestion.getScore() == null ? BigDecimal.ZERO : examQuestion.getScore();
            totalScore = totalScore.add(score);

            ExamQuestionRef ref = new ExamQuestionRef();
            ref.setQuestionId(examQuestion.getQuestionId());
            ref.setPoint(score);
            ref.setDisplayOrder(nextOrder);
            refs.add(ref);
        }

        if (!normalized.isEmpty()) {
            examQuestionRepository.saveAll(normalized);
        }

        exam.setQuestionRefs(refs);
        exam.setTotalScore(totalScore);
        exam.setSelectionConfig(newSelectionConfig(
                selectionMode,
                refs.size(),
                totalScore,
                quantityByType,
                quantityByDifficulty
        ));
        return examRepository.save(exam);
    }

    private void ensureCanManageExam(Account account, Exam exam) {
        if (account.getRole() == Role.ADMIN) {
            return;
        }
        if (account.getRole() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student cannot manage exams");
        }
        TeacherProfile teacher = getTeacherProfile(account);
        if (!teacher.getId().equals(exam.getTeacherId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher can only manage own exams");
        }
    }

    private void ensureNoSubmissions(String examId) {
        if (!examAttemptRepository.findByExamId(examId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exam already has attempts and cannot be modified this way"
            );
        }
    }

    private void ensureEditableStatus(Exam exam) {
        if (exam.getStatus() == ExamStatus.CLOSED || exam.getStatus() == ExamStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closed or cancelled exam cannot be updated");
        }
    }

    private void ensureQuestionMutableStatus(Exam exam) {
        if (exam.getStatus() == ExamStatus.CLOSED || exam.getStatus() == ExamStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Closed or cancelled exam questions cannot be modified"
            );
        }
    }

    private void ensureQuestionBankMatchesSubject(QuestionBank questionBank, String subjectId) {
        if (!questionBank.getSubjectId().equals(subjectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question bank does not belong to subject");
        }
    }

    private void ensureExamNameAvailable(String examName, String teacherId, String currentExamId) {
        examRepository.findByTitleAndTeacherId(examName.trim(), teacherId)
                .filter(existing -> currentExamId == null || !existing.getId().equals(currentExamId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam name already exists for teacher");
                });
    }

    private Subject getActiveSubject(String subjectId) {
        Subject subject = subjectRepository.findById(trimRequired(subjectId, "subjectId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found"));
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject is inactive");
        }
        return subject;
    }

    private QuestionBank getActiveQuestionBank(String questionBankId) {
        QuestionBank questionBank = questionBankRepository.findById(trimRequired(questionBankId, "questionBankId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question bank not found"));
        if (questionBank.getStatus() != QuestionBankStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question bank is inactive");
        }
        return questionBank;
    }

    private Question getActiveQuestion(String questionId) {
        Question question = questionRepository.findById(trimRequired(questionId, "questionId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (question.getStatus() != QuestionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is inactive");
        }
        return question;
    }

    private TeacherProfile validateActiveTeacher(String teacherId) {
        TeacherProfile teacher = teacherProfileRepository.findById(trimRequired(teacherId, "teacherId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        if (teacher.getStatus() != ProfileStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher profile is inactive");
        }
        return teacher;
    }

    private Exam getExamOrThrow(String id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private TeacherProfile getTeacherProfile(Account account) {
        return teacherProfileRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    private ExamResponse toResponse(Exam exam) {
        List<ExamQuestionResponse> questions = toExamQuestionResponses(exam);
        List<ClassResponse> classes = schoolClassRepository.findAllById(exam.getClassIds())
                .stream()
                .map(ClassResponse::from)
                .toList();
        SubjectResponse subject = subjectRepository.findById(exam.getSubjectId())
                .map(SubjectResponse::from)
                .orElse(null);
        TeacherProfileResponse teacher = teacherProfileRepository.findById(exam.getTeacherId())
                .map(profile -> TeacherProfileResponse.from(profile, getTeacherAccount(profile)))
                .orElse(null);
        QuestionBankResponse questionBank = questionBankRepository.findById(exam.getQuestionBankId())
                .map(bank -> QuestionBankResponse.from(bank, subject, teacher))
                .orElse(null);

        return ExamResponse.from(exam, questions, classes, subject, questionBank, teacher);
    }

    private List<ExamQuestionResponse> toExamQuestionResponses(Exam exam) {
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamIdOrderByOrderIndexAsc(exam.getId());
        if (!examQuestions.isEmpty()) {
            return examQuestions.stream()
                    .map(examQuestion -> ExamQuestionResponse.from(
                            examQuestion,
                            getQuestionResponse(examQuestion.getQuestionId())
                    ))
                    .toList();
        }

        if (exam.getQuestionRefs() == null || exam.getQuestionRefs().isEmpty()) {
            return List.of();
        }

        return exam.getQuestionRefs()
                .stream()
                .sorted(Comparator.comparingInt(ExamQuestionRef::getDisplayOrder))
                .map(ref -> ExamQuestionResponse.fromRef(exam.getId(), ref, getQuestionResponse(ref.getQuestionId())))
                .toList();
    }

    private QuestionResponse getQuestionResponse(String questionId) {
        return questionRepository.findById(questionId)
                .map(QuestionResponse::from)
                .orElse(null);
    }

    private AccountResponse getTeacherAccount(TeacherProfile profile) {
        return accountRepository.findById(profile.getAccountId())
                .filter(account -> !account.isDeleted())
                .map(AccountResponse::from)
                .orElse(null);
    }

    private void addExactCriteria(List<Criteria> criteria, String field, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        criteria.add(Criteria.where(field).is(value.trim()));
    }

    private void addRegexCriteria(List<Criteria> criteria, String field, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(value.trim()), Pattern.CASE_INSENSITIVE);
        criteria.add(Criteria.where(field).regex(pattern));
    }

    private String trimRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private enum PasswordChange {
        NONE,
        UPDATED,
        REMOVED
    }
}
