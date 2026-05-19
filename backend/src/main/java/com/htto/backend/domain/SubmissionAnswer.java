package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.GradingStatus;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "submission_answers")
@CompoundIndex(name = "uk_submission_question", def = "{'submissionId': 1, 'questionId': 1}", unique = true)
public class SubmissionAnswer extends AuditableDocument {

    @Indexed
    private String submissionId;

    @Indexed
    private String examId;

    @Indexed
    private String questionId;

    private Map<String, Object> studentAnswer = new HashMap<>();

    private Map<String, Object> correctAnswerSnapshot = new HashMap<>();

    private BigDecimal score;

    private GradingStatus gradingStatus = GradingStatus.UNGRADED;

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public Map<String, Object> getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(Map<String, Object> studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public Map<String, Object> getCorrectAnswerSnapshot() {
        return correctAnswerSnapshot;
    }

    public void setCorrectAnswerSnapshot(Map<String, Object> correctAnswerSnapshot) {
        this.correctAnswerSnapshot = correctAnswerSnapshot;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public GradingStatus getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(GradingStatus gradingStatus) {
        this.gradingStatus = gradingStatus;
    }
}
