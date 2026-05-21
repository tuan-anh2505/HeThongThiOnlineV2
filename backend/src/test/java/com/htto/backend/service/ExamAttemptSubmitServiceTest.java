package com.htto.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.AttemptAnswer;
import com.htto.backend.domain.DomainEnums.AttemptAnswerStatus;
import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.Exam;
import com.htto.backend.domain.ExamAttempt;
import com.htto.backend.domain.Question;
import com.htto.backend.domain.Role;
import com.htto.backend.domain.StudentProfile;
import com.htto.backend.domain.embedded.AnswerDefinition;
import com.htto.backend.domain.embedded.AnswerOption;
import com.htto.backend.domain.embedded.AttemptAnswerValue;
import com.htto.backend.domain.embedded.ExamAttemptOptionSnapshot;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import com.htto.backend.domain.embedded.ExamSettings;
import com.htto.backend.dto.response.SubmitAttemptResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.AttemptAnswerRepository;
import com.htto.backend.repository.ExamAttemptRepository;
import com.htto.backend.repository.ExamRepository;
import com.htto.backend.repository.QuestionRepository;
import com.htto.backend.repository.StudentProfileRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamAttemptSubmitServiceTest {

    private static final String USERNAME = "student";
    private static final String ACCOUNT_ID = "account-1";
    private static final String STUDENT_ID = "student-1";
    private static final String EXAM_ID = "exam-1";
    private static final String ATTEMPT_ID = "attempt-1";
    private static final String QUESTION_ID = "question-1";
    private static final String CORRECT_OPTION_ID = "A";
    private static final String WRONG_OPTION_ID = "B";

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    private ExamAttemptSubmitService service;

    @BeforeEach
    void setUp() {
        service = new ExamAttemptSubmitService(
                examAttemptRepository,
                attemptAnswerRepository,
                examRepository,
                questionRepository,
                accountRepository,
                studentProfileRepository
        );

        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setRole(Role.STUDENT);

        StudentProfile student = new StudentProfile();
        student.setId(STUDENT_ID);
        student.setAccountId(ACCOUNT_ID);

        when(accountRepository.findByUsernameAndDeletedFalse(USERNAME)).thenReturn(Optional.of(account));
        when(studentProfileRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(student));
        when(examAttemptRepository.save(any(ExamAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptAnswerRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitsAndGradesMultipleChoiceAttempt() {
        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(ATTEMPT_ID);
        attempt.setStudentId(STUDENT_ID);
        attempt.setExamId(EXAM_ID);
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
        attempt.setQuestionSnapshots(List.of(multipleChoiceSnapshot()));

        ExamSettings settings = new ExamSettings();
        settings.setShowScoreImmediately(true);
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setSettings(settings);

        Question question = new Question();
        question.setId(QUESTION_ID);
        question.setType(QuestionType.MULTIPLE_CHOICE);
        question.setScore(BigDecimal.ONE);
        AnswerDefinition answerDefinition = new AnswerDefinition();
        answerDefinition.setOptions(List.of(
                new AnswerOption(CORRECT_OPTION_ID, "Correct answer", true, 1),
                new AnswerOption(WRONG_OPTION_ID, "Wrong answer", false, 2)
        ));
        question.setAnswerDefinition(answerDefinition);

        AttemptAnswerValue answerValue = new AttemptAnswerValue();
        answerValue.setSelectedOptionId(CORRECT_OPTION_ID);
        AttemptAnswer answer = new AttemptAnswer();
        answer.setAttemptId(ATTEMPT_ID);
        answer.setExamId(EXAM_ID);
        answer.setQuestionId(QUESTION_ID);
        answer.setStudentAnswer(answerValue);

        when(examAttemptRepository.findByIdAndStudentId(ATTEMPT_ID, STUDENT_ID)).thenReturn(Optional.of(attempt));
        when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
        when(attemptAnswerRepository.findByAttemptId(ATTEMPT_ID)).thenReturn(List.of(answer));
        when(questionRepository.findAllById(List.of(QUESTION_ID))).thenReturn(List.of(question));

        SubmitAttemptResponse response = service.submitAttempt(ATTEMPT_ID, USERNAME);

        assertThat(response.submitted()).isTrue();
        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(attempt.getStatus()).isEqualTo(ExamAttemptStatus.SUBMITTED);
        assertThat(answer.getStatus()).isEqualTo(AttemptAnswerStatus.CORRECT);
        assertThat(answer.getScoreAchieved()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(answer.getIsCorrect()).isTrue();
    }

    private ExamAttemptQuestionSnapshot multipleChoiceSnapshot() {
        ExamAttemptQuestionSnapshot snapshot = new ExamAttemptQuestionSnapshot();
        snapshot.setQuestionId(QUESTION_ID);
        snapshot.setType(QuestionType.MULTIPLE_CHOICE);
        snapshot.setScore(BigDecimal.ONE);
        snapshot.setOrderIndex(1);

        ExamAttemptOptionSnapshot correctOption = new ExamAttemptOptionSnapshot();
        correctOption.setOptionId(CORRECT_OPTION_ID);
        correctOption.setContent("Correct answer");
        correctOption.setOrderIndex(1);

        ExamAttemptOptionSnapshot wrongOption = new ExamAttemptOptionSnapshot();
        wrongOption.setOptionId(WRONG_OPTION_ID);
        wrongOption.setContent("Wrong answer");
        wrongOption.setOrderIndex(2);

        snapshot.setOptions(List.of(correctOption, wrongOption));
        return snapshot;
    }
}
