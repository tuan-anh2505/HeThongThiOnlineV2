package com.htto.backend.seed;

import com.htto.backend.config.SeedProperties;
import com.htto.backend.domain.AdminProfile;
import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamSession;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.QuestionBank;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.Subject;
import com.htto.backend.domain.TeacherProfile;
import com.htto.backend.domain.TeachingAssignment;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.domain.embedded.AnswerDefinition;
import com.htto.backend.domain.embedded.AnswerOption;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import com.htto.backend.domain.embedded.ExamSettings;
import com.htto.backend.domain.embedded.QuestionSelectionConfig;
import com.htto.backend.repository.AdminProfileRepository;
import com.htto.backend.repository.ClassStudentRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.ExamSessionRepository;
import com.htto.backend.repository.QuestionBankRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.SchoolClassRepository;
import com.htto.backend.repository.StudentProfileRepository;
import com.htto.backend.repository.SubjectRepository;
import com.htto.backend.repository.TeacherProfileRepository;
import com.htto.backend.repository.TeachingAssignmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final String ADMIN_CODE = "ADM-SEED";
    private static final String TEACHER_CODE = "TCH-SEED";
    private static final String STUDENT_CODE = "STD-SEED";
    private static final String CLASS_CODE = "CLS-SEED";
    private static final String SUBJECT_CODE = "SUB-SEED";
    private static final String QUESTION_BANK_NAME = "Seed Question Bank";
    private static final String QUESTION_CONTENT = "Which keyword is used to define a class in Java?";
    private static final String EXAM_TITLE = "Seed Online Exam";

    private final SeedProperties seedProperties;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
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
            TeachingAssignmentRepository teachingAssignmentRepository,
            QuestionBankRepository questionBankRepository,
            QuestionRepository questionRepository,
            ExamRepository examRepository,
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
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.examSessionRepository = examSessionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }

        validateSeedPasswords();

        Account adminAccount = createAccountIfMissing(
                seedProperties.adminUsername(),
                seedProperties.adminEmail(),
                seedProperties.adminPassword(),
                "Seed Admin",
                Role.ADMIN
        );
        Account teacherAccount = createAccountIfMissing(
                seedProperties.teacherUsername(),
                seedProperties.teacherEmail(),
                seedProperties.teacherPassword(),
                "Seed Teacher",
                Role.TEACHER
        );
        Account studentAccount = createAccountIfMissing(
                seedProperties.studentUsername(),
                seedProperties.studentEmail(),
                seedProperties.studentPassword(),
                "Seed Student",
                Role.STUDENT
        );

        AdminProfile admin = createAdminProfileIfMissing(adminAccount);
        TeacherProfile teacher = createTeacherProfileIfMissing(teacherAccount);
        StudentProfile student = createStudentProfileIfMissing(studentAccount);
        Subject subject = createSubjectIfMissing();
        SchoolClass schoolClass = createClassIfMissing(teacher);

        attachStudentToClass(student, schoolClass);
        attachTeacherScope(teacher, subject, schoolClass);
        createTeachingAssignmentIfMissing(schoolClass, subject, teacher);

        QuestionBank questionBank = createQuestionBankIfMissing(subject, teacher);
        Question question = createQuestionIfMissing(questionBank);
        Exam exam = createExamIfMissing(schoolClass, subject, teacher, questionBank, question);
        createExamSessionIfMissing(exam);

        if (!StringUtils.hasText(admin.getId())) {
            throw new IllegalStateException("Seed admin profile was not persisted");
        }
    }

    private void validateSeedPasswords() {
        requireText(seedProperties.adminPassword(), "SEED_ADMIN_PASSWORD");
        requireText(seedProperties.teacherPassword(), "SEED_TEACHER_PASSWORD");
        requireText(seedProperties.studentPassword(), "SEED_STUDENT_PASSWORD");
    }

    private void requireText(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(envName + " is required when SEED_ENABLED=true");
        }
    }

    private Account createAccountIfMissing(
            String username,
            String email,
            String password,
            String fullName,
            Role role
    ) {
        return accountRepository.findByUsernameOrEmail(username, email)
                .map(existing -> ensureRole(existing, role))
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setUsername(username);
                    account.setEmail(email);
                    account.setPassword(passwordEncoder.encode(password));
                    account.setFullName(fullName);
                    account.setRole(role);
                    return accountRepository.save(account);
                });
    }

    private Account ensureRole(Account account, Role role) {
        if (account.getRole() == role) {
            return account;
        }
        account.setRole(role);
        return accountRepository.save(account);
    }

    private AdminProfile createAdminProfileIfMissing(Account account) {
        return adminProfileRepository.findByAccountId(account.getId())
                .or(() -> adminProfileRepository.findByAdminCode(ADMIN_CODE))
                .orElseGet(() -> {
                    AdminProfile profile = new AdminProfile();
                    profile.setAdminCode(ADMIN_CODE);
                    profile.setAccountId(account.getId());
                    return adminProfileRepository.save(profile);
                });
    }

    private TeacherProfile createTeacherProfileIfMissing(Account account) {
        return teacherProfileRepository.findByAccountId(account.getId())
                .or(() -> teacherProfileRepository.findByTeacherCode(TEACHER_CODE))
                .orElseGet(() -> {
                    TeacherProfile profile = new TeacherProfile();
                    profile.setTeacherCode(TEACHER_CODE);
                    profile.setAccountId(account.getId());
                    return teacherProfileRepository.save(profile);
                });
    }

    private StudentProfile createStudentProfileIfMissing(Account account) {
        return studentProfileRepository.findByAccountId(account.getId())
                .or(() -> studentProfileRepository.findByStudentCode(STUDENT_CODE))
                .orElseGet(() -> {
                    StudentProfile profile = new StudentProfile();
                    profile.setStudentCode(STUDENT_CODE);
                    profile.setAccountId(account.getId());
                    return studentProfileRepository.save(profile);
                });
    }

    private Subject createSubjectIfMissing() {
        return subjectRepository.findBySubjectCode(SUBJECT_CODE)
                .orElseGet(() -> {
                    Subject subject = new Subject();
                    subject.setSubjectCode(SUBJECT_CODE);
                    subject.setName("Programming Basics");
                    subject.setDescription("Seed subject for online exam testing");
                    return subjectRepository.save(subject);
                });
    }

    private SchoolClass createClassIfMissing(TeacherProfile teacher) {
        return schoolClassRepository.findByClassCode(CLASS_CODE)
                .orElseGet(() -> {
                    SchoolClass schoolClass = new SchoolClass();
                    schoolClass.setClassCode(CLASS_CODE);
                    schoolClass.setName("Seed Class");
                    schoolClass.setTeacherId(teacher.getId());
                    schoolClass.setStudentCount(0);
                    schoolClass.setStatus(ClassStatus.ACTIVE);
                    return schoolClassRepository.save(schoolClass);
                });
    }

    private void attachStudentToClass(StudentProfile student, SchoolClass schoolClass) {
        classStudentRepository.findByClassIdAndStudentId(schoolClass.getId(), student.getId())
                .orElseGet(() -> {
                    ClassStudent classStudent = new ClassStudent();
                    classStudent.setClassId(schoolClass.getId());
                    classStudent.setStudentId(student.getId());
                    classStudent.setAddedAt(Instant.now());
                    classStudent.setStatus(EnrollmentStatus.STUDYING);
                    return classStudentRepository.save(classStudent);
                });

        if (!schoolClass.getId().equals(student.getMainClassId())) {
            student.setMainClassId(schoolClass.getId());
        }
        if (!student.getClassIds().contains(schoolClass.getId())) {
            student.getClassIds().add(schoolClass.getId());
        }
        studentProfileRepository.save(student);

        int studentCount = classStudentRepository.findByClassId(schoolClass.getId()).size();
        if (schoolClass.getStudentCount() != studentCount) {
            schoolClass.setStudentCount(studentCount);
            schoolClassRepository.save(schoolClass);
        }
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
        if (changed) {
            teacherProfileRepository.save(teacher);
        }
    }

    private void createTeachingAssignmentIfMissing(
            SchoolClass schoolClass,
            Subject subject,
            TeacherProfile teacher
    ) {
        teachingAssignmentRepository
                .findByClassIdAndSubjectIdAndTeacherId(schoolClass.getId(), subject.getId(), teacher.getId())
                .orElseGet(() -> {
                    TeachingAssignment assignment = new TeachingAssignment();
                    assignment.setClassId(schoolClass.getId());
                    assignment.setSubjectId(subject.getId());
                    assignment.setTeacherId(teacher.getId());
                    assignment.setSemester("Seed Semester");
                    assignment.setSchoolYear("2026");
                    assignment.setStatus(AssignmentStatus.ACTIVE);
                    return teachingAssignmentRepository.save(assignment);
                });
    }

    private QuestionBank createQuestionBankIfMissing(Subject subject, TeacherProfile teacher) {
        return questionBankRepository.findByNameAndTeacherId(QUESTION_BANK_NAME, teacher.getId())
                .orElseGet(() -> {
                    QuestionBank questionBank = new QuestionBank();
                    questionBank.setName(QUESTION_BANK_NAME);
                    questionBank.setDescription("Seed question bank for smoke testing");
                    questionBank.setSubjectId(subject.getId());
                    questionBank.setTeacherId(teacher.getId());
                    questionBank.setStatus(QuestionBankStatus.ACTIVE);
                    return questionBankRepository.save(questionBank);
                });
    }

    private Question createQuestionIfMissing(QuestionBank questionBank) {
        return questionRepository.findByQuestionBankIdAndContent(questionBank.getId(), QUESTION_CONTENT)
                .orElseGet(() -> {
                    Question question = new Question();
                    question.setQuestionBankId(questionBank.getId());
                    question.setType(QuestionType.SINGLE_CHOICE);
                    question.setContent(QUESTION_CONTENT);
                    question.setPoint(BigDecimal.ONE);
                    question.setDifficulty(Difficulty.EASY);
                    question.setTopic("Java");
                    question.setStatus(QuestionStatus.ACTIVE);
                    question.setAnswerDefinition(createSingleChoiceAnswer());
                    return questionRepository.save(question);
                });
    }

    private AnswerDefinition createSingleChoiceAnswer() {
        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setOptions(List.of(
                new AnswerOption("A", "class", true, 1),
                new AnswerOption("B", "function", false, 2),
                new AnswerOption("C", "module", false, 3),
                new AnswerOption("D", "define", false, 4)
        ));
        return answerDefinition;
    }

    private Exam createExamIfMissing(
            SchoolClass schoolClass,
            Subject subject,
            TeacherProfile teacher,
            QuestionBank questionBank,
            Question question
    ) {
        return examRepository.findByTitleAndTeacherId(EXAM_TITLE, teacher.getId())
                .orElseGet(() -> {
                    Exam exam = new Exam();
                    exam.setTitle(EXAM_TITLE);
                    exam.setClassIds(new ArrayList<>(List.of(schoolClass.getId())));
                    exam.setSubjectId(subject.getId());
                    exam.setQuestionBankId(questionBank.getId());
                    exam.setTeacherId(teacher.getId());
                    exam.setDurationMinutes(30);
                    exam.setMaxAttempts(1);
                    exam.setTotalScore(BigDecimal.ONE);
                    exam.setSettings(createExamSettings());
                    exam.setSelectionConfig(createSelectionConfig());
                    exam.setQuestionRefs(List.of(createQuestionRef(question)));
                    exam.setStatus(ExamStatus.PUBLISHED);
                    return examRepository.save(exam);
                });
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

    private QuestionSelectionConfig createSelectionConfig() {
        QuestionSelectionConfig selectionConfig = new QuestionSelectionConfig();
        selectionConfig.setTotalQuestions(1);
        selectionConfig.setTotalScore(BigDecimal.ONE);
        return selectionConfig;
    }

    private ExamQuestionRef createQuestionRef(Question question) {
        ExamQuestionRef questionRef = new ExamQuestionRef();
        questionRef.setQuestionId(question.getId());
        questionRef.setPoint(question.getPoint());
        questionRef.setDisplayOrder(1);
        return questionRef;
    }

    private void createExamSessionIfMissing(Exam exam) {
        if (!examSessionRepository.findByExamId(exam.getId()).isEmpty()) {
            return;
        }

        ExamSession session = new ExamSession();
        session.setExamId(exam.getId());
        session.setStartAt(Instant.now().minus(1, ChronoUnit.HOURS));
        session.setEndAt(Instant.now().plus(7, ChronoUnit.DAYS));
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        examSessionRepository.save(session);
    }
}
