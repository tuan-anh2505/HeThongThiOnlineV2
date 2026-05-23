package com.htto.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.ExamQuestion;
import com.htto.backend.domain.ExamSession;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExamAttemptServicePasswordTest {

    private static final String USERNAME = "student";
    private static final String ACCOUNT_ID = "account-1";
    private static final String STUDENT_ID = "student-1";
    private static final String EXAM_ID = "exam-1";
    private static final String CLASS_ID = "class-1";
    private static final String SESSION_ID = "session-1";
    private static final String QUESTION_ID = "question-1";

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ExamAttemptSubmitService examAttemptSubmitService;

    @Mock
    private SystemLogService systemLogService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private ExamAttemptService service;
    private Exam exam;

    @BeforeEach
    void setUp() {
        service = new ExamAttemptService(
                examAttemptRepository,
                examRepository,
                examSessionRepository,
                examQuestionRepository,
                questionRepository,
                accountRepository,
                studentProfileRepository,
                classStudentRepository,
                passwordEncoder,
                examAttemptSubmitService,
                systemLogService
        );

        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setRole(Role.STUDENT);

        StudentProfile student = new StudentProfile();
        student.setId(STUDENT_ID);
        student.setAccountId(ACCOUNT_ID);

        exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setTitle("Midterm");
        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setClassIds(List.of(CLASS_ID));
        exam.setDurationMinutes(30);
        exam.setMaxAttempts(2);

        ExamSession session = new ExamSession();
        session.setId(SESSION_ID);
        session.setExamId(EXAM_ID);
        session.setStartTime(Instant.now().minusSeconds(60));
        session.setEndTime(Instant.now().plusSeconds(3600));
        session.setStatus(ExamSessionStatus.IN_PROGRESS);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setClassId(CLASS_ID);
        classStudent.setStudentId(STUDENT_ID);
        classStudent.setStatus(EnrollmentStatus.ACTIVE);

        ExamQuestion examQuestion = new ExamQuestion();
        examQuestion.setExamId(EXAM_ID);
        examQuestion.setQuestionId(QUESTION_ID);
        examQuestion.setScore(BigDecimal.ONE);
        examQuestion.setOrderIndex(1);

        Question question = new Question();
        question.setId(QUESTION_ID);
        question.setContent("Question");
        question.setScore(BigDecimal.ONE);

        lenient().when(accountRepository.findByUsernameAndDeletedFalse(USERNAME)).thenReturn(Optional.of(account));
        lenient().when(studentProfileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(student));
        lenient().when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        lenient().when(examSessionRepository.findByExamIdOrderByStartTimeAsc(EXAM_ID)).thenReturn(List.of(session));
        lenient().when(classStudentRepository.findByStudentIdAndStatus(STUDENT_ID, EnrollmentStatus.ACTIVE))
                .thenReturn(List.of(classStudent));
        lenient().when(examAttemptRepository.findByExamIdAndStudentIdOrderByAttemptNumberAsc(EXAM_ID, STUDENT_ID))
                .thenReturn(List.of());
        lenient().when(examQuestionRepository.findByExamIdOrderByOrderIndexAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion));
        lenient().when(questionRepository.findAllById(List.of(QUESTION_ID))).thenReturn(List.of(question));
        lenient().when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> {
            ExamAttempt attempt = invocation.getArgument(0);
            attempt.setId("attempt-1");
            return attempt;
        });
    }

    @Test
    void startsExamWithoutPassword() {
        exam.setHasPassword(false);

        ExamAttemptResponse response = service.startExam(EXAM_ID, null, USERNAME);

        assertThat(response.attemptId()).isEqualTo("attempt-1");
        assertThat(response.questions()).hasSize(1);
    }

    @Test
    void rejectsMissingPasswordWhenExamRequiresPassword() {
        exam.setHasPassword(true);
        exam.setExamPasswordHash(passwordEncoder.encode("secret"));

        assertThatThrownBy(() -> service.startExam(EXAM_ID, new StartExamRequest(null), USERNAME))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bài thi yêu cầu mật khẩu");
    }

    @Test
    void rejectsWrongPasswordWhenExamRequiresPassword() {
        exam.setHasPassword(true);
        exam.setExamPasswordHash(passwordEncoder.encode("secret"));

        assertThatThrownBy(() -> service.startExam(EXAM_ID, new StartExamRequest("wrong"), USERNAME))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Mật khẩu bài thi không đúng");
        verify(systemLogService).log(
                ACCOUNT_ID,
                "WRONG_EXAM_PASSWORD",
                "EXAM",
                EXAM_ID,
                "Student entered wrong exam password, examId=" + EXAM_ID
        );
    }

    @Test
    void startsExamWithCorrectPassword() {
        exam.setHasPassword(true);
        exam.setExamPasswordHash(passwordEncoder.encode("secret"));

        ExamAttemptResponse response = service.startExam(EXAM_ID, new StartExamRequest("secret"), USERNAME);

        assertThat(response.attemptId()).isEqualTo("attempt-1");
        assertThat(response.questions()).hasSize(1);
    }
}
