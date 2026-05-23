package com.htto.backend.seed;

import com.htto.backend.config.SeedProperties;
import com.htto.backend.domain.Account;
import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.AdminProfile;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AcademicStatus;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
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
import com.htto.backend.domain.ExamSession;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.QuestionBank;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.domain.embedded.AnswerDefinition;
import com.htto.backend.domain.embedded.AnswerOption;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import com.htto.backend.domain.embedded.ExamSettings;
import com.htto.backend.domain.embedded.FillBlankAnswer;
import com.htto.backend.domain.embedded.MatchingPair;
import com.htto.backend.domain.embedded.QuestionSelectionConfig;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AdminProfileRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ClassSubjectTeacherRepository;
import com.htto.backend.repository.ExamQuestionRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.ExamSessionRepository;
import com.htto.backend.repository.QuestionBankRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final String ADMIN_CODE = "ADM001";
    private static final String TEACHER_CODE = "GV001";
    private static final String STUDENT_1_CODE = "SV001";
    private static final String STUDENT_2_CODE = "SV002";
    private static final String CLASS_CODE = "CNTT1";
    private static final String CLASS_NAME = "Lớp CNTT 1";
    private static final String SUBJECT_CODE = "LTM";
    private static final String SUBJECT_NAME = "Lập trình mạng";
    private static final String QUESTION_BANK_NAME = "Ngân hàng câu hỏi Lập trình mạng";
    private static final String EXAM_TITLE = "Bài thi thử Lập trình mạng";
    private static final BigDecimal DEFAULT_SCORE = BigDecimal.ONE;

    private final SeedProperties seedProperties;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectTeacherRepository classSubjectTeacherRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamSessionRepository examSessionRepository;

    public DatabaseSeeder(
            SeedProperties seedProperties,
            PasswordEncoder passwordEncoder,
            AccountRepository accountRepository,
            AdminProfileRepository adminProfileRepository,
            TeacherProfileRepository teacherProfileRepository,
            StudentProfileRepository studentProfileRepository,
            SchoolClassRepository schoolClassRepository,
            ClassStudentRepository classStudentRepository,
            SubjectRepository subjectRepository,
            ClassSubjectTeacherRepository classSubjectTeacherRepository,
            QuestionBankRepository questionBankRepository,
            QuestionRepository questionRepository,
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamSessionRepository examSessionRepository
    ) {
        this.seedProperties = seedProperties;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.classStudentRepository = classStudentRepository;
        this.subjectRepository = subjectRepository;
        this.classSubjectTeacherRepository = classSubjectTeacherRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examSessionRepository = examSessionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }

        validateSeedPasswords();

        Account adminAccount = createAccount(
                seedProperties.adminUsername(),
                seedProperties.adminEmail(),
                seedProperties.adminPassword(),
                "Quản trị hệ thống",
                Role.ADMIN
        );
        Account teacherAccount = createAccount(
                seedProperties.teacherUsername(),
                seedProperties.teacherEmail(),
                seedProperties.teacherPassword(),
                "Giảng viên 01",
                Role.TEACHER
        );
        Account student1Account = createAccount(
                seedProperties.studentUsername(),
                seedProperties.studentEmail(),
                seedProperties.studentPassword(),
                "Sinh viên 01",
                Role.STUDENT
        );
        Account student2Account = createAccount(
                seedProperties.student2Username(),
                seedProperties.student2Email(),
                seedProperties.student2Password(),
                "Sinh viên 02",
                Role.STUDENT
        );

        createAdminProfile(adminAccount);
        TeacherProfile teacher = createTeacherProfile(teacherAccount);
        StudentProfile student1 = createStudentProfile(student1Account, STUDENT_1_CODE);
        StudentProfile student2 = createStudentProfile(student2Account, STUDENT_2_CODE);

        Subject subject = createSubject();
        SchoolClass schoolClass = createClass(teacher);
        attachStudentToClass(student1, schoolClass);
        attachStudentToClass(student2, schoolClass);
        updateClassStudentCount(schoolClass);
        attachTeacherScope(teacher, subject, schoolClass);
        createClassSubjectTeacher(schoolClass, subject, teacher);

        QuestionBank questionBank = createQuestionBank(subject, teacher);
        List<Question> questions = createQuestions(questionBank);
        Exam exam = createExam(schoolClass, subject, teacher, questionBank, questions);
        upsertExamQuestions(exam, questions);
        createExamSession(exam);
    }

    private void validateSeedPasswords() {
        requireText(seedProperties.adminPassword(), "SEED_ADMIN_PASSWORD");
        requireText(seedProperties.teacherPassword(), "SEED_TEACHER_PASSWORD");
        requireText(seedProperties.studentPassword(), "SEED_STUDENT_PASSWORD");
        requireText(seedProperties.student2Password(), "SEED_STUDENT2_PASSWORD");
    }

    private void requireText(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(envName + " is required when SEED_ENABLED=true");
        }
    }

    private Account createAccount(
            String username,
            String email,
            String password,
            String fullName,
            Role role
    ) {
        Account account = accountRepository.findByUsernameOrEmail(username, email)
                .orElseGet(Account::new);
        account.setUsername(username);
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setFullName(fullName);
        account.setRole(role);
        account.setStatus(AccountStatus.ACTIVE);
        account.setDeleted(false);
        account.setDeletedAt(null);
        return accountRepository.save(account);
    }

    private AdminProfile createAdminProfile(Account account) {
        AdminProfile profile = adminProfileRepository.findByAccountId(account.getId())
                .or(() -> adminProfileRepository.findByAdminCode(ADMIN_CODE))
                .orElseGet(AdminProfile::new);
        profile.setAdminCode(ADMIN_CODE);
        profile.setAccountId(account.getId());
        return adminProfileRepository.save(profile);
    }

    private TeacherProfile createTeacherProfile(Account account) {
        TeacherProfile profile = teacherProfileRepository.findByAccountId(account.getId())
                .or(() -> teacherProfileRepository.findByTeacherCode(TEACHER_CODE))
                .orElseGet(TeacherProfile::new);
        profile.setTeacherCode(TEACHER_CODE);
        profile.setAccountId(account.getId());
        profile.setStatus(ProfileStatus.ACTIVE);
        return teacherProfileRepository.save(profile);
    }

    private StudentProfile createStudentProfile(Account account, String studentCode) {
        StudentProfile profile = studentProfileRepository.findByAccountId(account.getId())
                .or(() -> studentProfileRepository.findByStudentCode(studentCode))
                .orElseGet(StudentProfile::new);
        profile.setStudentCode(studentCode);
        profile.setAccountId(account.getId());
        profile.setStatus(AcademicStatus.ACTIVE);
        return studentProfileRepository.save(profile);
    }

    private Subject createSubject() {
        Subject subject = subjectRepository.findBySubjectCode(SUBJECT_CODE)
                .orElseGet(Subject::new);
        subject.setSubjectCode(SUBJECT_CODE);
        subject.setSubjectName(SUBJECT_NAME);
        subject.setDescription("Môn thi mẫu dùng để kiểm thử luồng hệ thống thi online");
        subject.setStatus(SubjectStatus.ACTIVE);
        return subjectRepository.save(subject);
    }

    private SchoolClass createClass(TeacherProfile teacher) {
        SchoolClass schoolClass = schoolClassRepository.findByClassCode(CLASS_CODE)
                .orElseGet(SchoolClass::new);
        schoolClass.setClassCode(CLASS_CODE);
        schoolClass.setClassName(CLASS_NAME);
        schoolClass.setTeacherId(teacher.getId());
        schoolClass.setStatus(ClassStatus.ACTIVE);
        return schoolClassRepository.save(schoolClass);
    }

    private void attachStudentToClass(StudentProfile student, SchoolClass schoolClass) {
        ClassStudent classStudent = classStudentRepository
                .findByClassIdAndStudentId(schoolClass.getId(), student.getId())
                .orElseGet(ClassStudent::new);
        classStudent.setClassId(schoolClass.getId());
        classStudent.setStudentId(student.getId());
        if (classStudent.getJoinedAt() == null) {
            classStudent.setJoinedAt(Instant.now());
        }
        classStudent.setStatus(EnrollmentStatus.ACTIVE);
        classStudentRepository.save(classStudent);

        student.setMainClassId(schoolClass.getId());
        if (!student.getClassIds().contains(schoolClass.getId())) {
            student.getClassIds().add(schoolClass.getId());
        }
        studentProfileRepository.save(student);
    }

    private void updateClassStudentCount(SchoolClass schoolClass) {
        int studentCount = classStudentRepository
                .findByClassIdAndStatus(schoolClass.getId(), EnrollmentStatus.ACTIVE)
                .size();
        schoolClass.setStudentCount(studentCount);
        schoolClassRepository.save(schoolClass);
    }

    private void attachTeacherScope(TeacherProfile teacher, Subject subject, SchoolClass schoolClass) {
        boolean changed = false;
        if (!teacher.getClassIds().contains(schoolClass.getId())) {
            teacher.getClassIds().add(schoolClass.getId());
            changed = true;
        }
        if (!teacher.getSubjectIds().contains(subject.getId())) {
            teacher.getSubjectIds().add(subject.getId());
            changed = true;
        }
        if (teacher.getStatus() != ProfileStatus.ACTIVE) {
            teacher.setStatus(ProfileStatus.ACTIVE);
            changed = true;
        }
        if (changed) {
            teacherProfileRepository.save(teacher);
        }
    }

    private void createClassSubjectTeacher(
            SchoolClass schoolClass,
            Subject subject,
            TeacherProfile teacher
    ) {
        ClassSubjectTeacher assignment = classSubjectTeacherRepository
                .findByClassIdAndSubjectIdAndTeacherIdAndStatus(
                        schoolClass.getId(),
                        subject.getId(),
                        teacher.getId(),
                        AssignmentStatus.ACTIVE
                )
                .orElseGet(ClassSubjectTeacher::new);
        assignment.setClassId(schoolClass.getId());
        assignment.setSubjectId(subject.getId());
        assignment.setTeacherId(teacher.getId());
        assignment.setSemester("Học kỳ 1");
        assignment.setSchoolYear("2026");
        assignment.setStatus(AssignmentStatus.ACTIVE);
        classSubjectTeacherRepository.save(assignment);
    }

    private QuestionBank createQuestionBank(Subject subject, TeacherProfile teacher) {
        QuestionBank questionBank = questionBankRepository
                .findByNameAndTeacherId(QUESTION_BANK_NAME, teacher.getId())
                .orElseGet(QuestionBank::new);
        questionBank.setName(QUESTION_BANK_NAME);
        questionBank.setDescription("Ngân hàng câu hỏi mẫu cho môn Lập trình mạng");
        questionBank.setSubjectId(subject.getId());
        questionBank.setTeacherId(teacher.getId());
        questionBank.setStatus(QuestionBankStatus.ACTIVE);
        return questionBankRepository.save(questionBank);
    }

    private List<Question> createQuestions(QuestionBank questionBank) {
        return List.of(
                upsertQuestion(
                        questionBank,
                        QuestionType.TRUE_FALSE,
                        "TCP là giao thức hướng kết nối.",
                        DEFAULT_SCORE,
                        Difficulty.EASY,
                        "Giao thức mạng",
                        trueFalseAnswer(true)
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.TRUE_FALSE,
                        "UDP đảm bảo truyền dữ liệu tin cậy hơn TCP.",
                        DEFAULT_SCORE,
                        Difficulty.EASY,
                        "Giao thức mạng",
                        trueFalseAnswer(false)
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.MULTIPLE_CHOICE,
                        "HTTP hoạt động chủ yếu ở tầng nào trong mô hình TCP/IP?",
                        DEFAULT_SCORE,
                        Difficulty.EASY,
                        "HTTP",
                        multipleChoiceAnswer(List.of(
                                new AnswerOption("A", "Application", true, 1),
                                new AnswerOption("B", "Transport", false, 2),
                                new AnswerOption("C", "Internet", false, 3),
                                new AnswerOption("D", "Network Access", false, 4)
                        ))
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.MULTIPLE_CHOICE,
                        "Cổng mặc định của HTTPS là gì?",
                        DEFAULT_SCORE,
                        Difficulty.EASY,
                        "HTTP",
                        multipleChoiceAnswer(List.of(
                                new AnswerOption("A", "80", false, 1),
                                new AnswerOption("B", "21", false, 2),
                                new AnswerOption("C", "443", true, 3),
                                new AnswerOption("D", "25", false, 4)
                        ))
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.MULTIPLE_CHOICE,
                        "Giao thức nào dùng để phân giải tên miền thành địa chỉ IP?",
                        DEFAULT_SCORE,
                        Difficulty.MEDIUM,
                        "DNS",
                        multipleChoiceAnswer(List.of(
                                new AnswerOption("A", "DHCP", false, 1),
                                new AnswerOption("B", "DNS", true, 2),
                                new AnswerOption("C", "SMTP", false, 3),
                                new AnswerOption("D", "FTP", false, 4)
                        ))
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.MULTIPLE_CHOICE,
                        "Địa chỉ IPv4 có độ dài bao nhiêu bit?",
                        DEFAULT_SCORE,
                        Difficulty.MEDIUM,
                        "IP",
                        multipleChoiceAnswer(List.of(
                                new AnswerOption("A", "16 bit", false, 1),
                                new AnswerOption("B", "32 bit", true, 2),
                                new AnswerOption("C", "64 bit", false, 3),
                                new AnswerOption("D", "128 bit", false, 4)
                        ))
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.FILL_BLANK,
                        "HTTP là viết tắt của ______",
                        DEFAULT_SCORE,
                        Difficulty.MEDIUM,
                        "HTTP",
                        fillBlankAnswer(List.of(
                                "HyperText Transfer Protocol",
                                "Hyper Text Transfer Protocol"
                        ))
                ),
                upsertQuestion(
                        questionBank,
                        QuestionType.MATCHING,
                        "Nối giao thức với mô tả phù hợp.",
                        BigDecimal.valueOf(2),
                        Difficulty.HARD,
                        "Giao thức mạng",
                        matchingAnswer(List.of(
                                matchingPair("M1", "HTTP", "Giao thức truyền siêu văn bản", 1),
                                matchingPair("M2", "DNS", "Phân giải tên miền", 2),
                                matchingPair("M3", "IP", "Định tuyến gói tin", 3),
                                matchingPair("M4", "TCP", "Truyền dữ liệu hướng kết nối", 4)
                        ))
                )
        );
    }

    private Question upsertQuestion(
            QuestionBank questionBank,
            QuestionType type,
            String content,
            BigDecimal score,
            Difficulty difficulty,
            String topic,
            AnswerDefinition answerDefinition
    ) {
        Question question = questionRepository
                .findByQuestionBankIdAndContent(questionBank.getId(), content)
                .orElseGet(Question::new);
        question.setQuestionBankId(questionBank.getId());
        question.setType(type);
        question.setContent(content);
        question.setScore(score);
        question.setDifficulty(difficulty);
        question.setTopic(topic);
        question.setStatus(QuestionStatus.ACTIVE);
        question.setAnswerDefinition(answerDefinition);
        return questionRepository.save(question);
    }

    private AnswerDefinition trueFalseAnswer(boolean correctAnswer) {
        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setTrueFalseAnswer(correctAnswer);
        return answerDefinition;
    }

    private AnswerDefinition multipleChoiceAnswer(List<AnswerOption> options) {
        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setOptions(options);
        return answerDefinition;
    }

    private AnswerDefinition fillBlankAnswer(List<String> acceptedAnswers) {
        FillBlankAnswer fillBlankAnswer = new FillBlankAnswer();
        fillBlankAnswer.setAnswerId("FB1");
        fillBlankAnswer.setAcceptedAnswers(acceptedAnswers);
        fillBlankAnswer.setIgnoreCase(true);
        fillBlankAnswer.setIgnoreAccent(true);
        fillBlankAnswer.setTrimSpace(true);

        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setFillBlankAnswer(fillBlankAnswer);
        return answerDefinition;
    }

    private AnswerDefinition matchingAnswer(List<MatchingPair> pairs) {
        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setMatchingPairs(pairs);
        return answerDefinition;
    }

    private MatchingPair matchingPair(String matchingId, String leftText, String rightText, int orderIndex) {
        MatchingPair pair = new MatchingPair();
        pair.setMatchingId(matchingId);
        pair.setLeftText(leftText);
        pair.setRightText(rightText);
        pair.setOrderIndex(orderIndex);
        return pair;
    }

    private Exam createExam(
            SchoolClass schoolClass,
            Subject subject,
            TeacherProfile teacher,
            QuestionBank questionBank,
            List<Question> questions
    ) {
        BigDecimal totalScore = questions.stream()
                .map(Question::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Exam exam = examRepository.findByTitleAndTeacherId(EXAM_TITLE, teacher.getId())
                .orElseGet(Exam::new);
        exam.setTitle(EXAM_TITLE);
        exam.setClassIds(new ArrayList<>(List.of(schoolClass.getId())));
        exam.setSubjectId(subject.getId());
        exam.setQuestionBankId(questionBank.getId());
        exam.setTeacherId(teacher.getId());
        exam.setDurationMinutes(15);
        exam.setMaxAttempts(1);
        exam.setTotalScore(totalScore);
        exam.setSettings(createExamSettings());
        exam.setSelectionConfig(createSelectionConfig(questions, totalScore));
        exam.setQuestionRefs(createQuestionRefs(questions));
        exam.setHasPassword(false);
        exam.setExamPasswordHash(null);
        exam.setResultStatus(ResultPublishStatus.NOT_PUBLISHED);
        exam.setStatus(ExamStatus.PUBLISHED);
        return examRepository.save(exam);
    }

    private ExamSettings createExamSettings() {
        ExamSettings settings = new ExamSettings();
        settings.setShowScoreImmediately(true);
        settings.setAllowReview(true);
        settings.setShowCorrectAnswers(true);
        settings.setShuffleQuestions(false);
        settings.setShuffleAnswers(false);
        return settings;
    }

    private QuestionSelectionConfig createSelectionConfig(List<Question> questions, BigDecimal totalScore) {
        QuestionSelectionConfig selectionConfig = new QuestionSelectionConfig();
        selectionConfig.setSelectionMode(SelectionMode.MANUAL);
        selectionConfig.setTotalQuestions(questions.size());
        selectionConfig.setTotalScore(totalScore);
        selectionConfig.setQuantityByType(countByType(questions));
        selectionConfig.setQuantityByDifficulty(countByDifficulty(questions));
        return selectionConfig;
    }

    private Map<QuestionType, Integer> countByType(List<Question> questions) {
        Map<QuestionType, Integer> counts = new EnumMap<>(QuestionType.class);
        for (Question question : questions) {
            counts.merge(question.getType(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Difficulty, Integer> countByDifficulty(List<Question> questions) {
        Map<Difficulty, Integer> counts = new EnumMap<>(Difficulty.class);
        for (Question question : questions) {
            counts.merge(question.getDifficulty(), 1, Integer::sum);
        }
        return counts;
    }

    private List<ExamQuestionRef> createQuestionRefs(List<Question> questions) {
        List<ExamQuestionRef> refs = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            ExamQuestionRef questionRef = new ExamQuestionRef();
            questionRef.setQuestionId(question.getId());
            questionRef.setPoint(question.getScore());
            questionRef.setDisplayOrder(i + 1);
            refs.add(questionRef);
        }
        return refs;
    }

    private void upsertExamQuestions(Exam exam, List<Question> questions) {
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            ExamQuestion examQuestion = examQuestionRepository
                    .findByExamIdAndQuestionId(exam.getId(), question.getId())
                    .orElseGet(ExamQuestion::new);
            examQuestion.setExamId(exam.getId());
            examQuestion.setQuestionId(question.getId());
            examQuestion.setScore(question.getScore());
            examQuestion.setOrderIndex(i + 1);
            examQuestionRepository.save(examQuestion);
        }
    }

    private void createExamSession(Exam exam) {
        ExamSession session = examSessionRepository.findByExamIdOrderByStartTimeAsc(exam.getId())
                .stream()
                .findFirst()
                .orElseGet(ExamSession::new);
        session.setExamId(exam.getId());
        session.setStartTime(Instant.now().minus(15, ChronoUnit.MINUTES));
        session.setEndTime(Instant.now().plus(7, ChronoUnit.DAYS));
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.save(session);
    }
}
