package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.AttemptAnswerStatus;
import com.htto.backend.domain.embedded.AttemptAnswerValue;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attempt_answers")
@CompoundIndex(name = "uk_attempt_answer_question", def = "{'attemptId': 1, 'questionId': 1}", unique = true)
public class AttemptAnswer extends AuditableDocument {

    @Indexed
    private String attemptId;

    @Indexed
    private String examId;

    @Indexed
    private String questionId;

    private AttemptAnswerValue studentAnswer;

    private Map<String, Object> correctAnswerSnapshot = new HashMap<>();

    private BigDecimal scoreAchieved;

    private Boolean isCorrect;

    @Indexed
    private AttemptAnswerStatus status = AttemptAnswerStatus.NOT_GRADED;

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
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

    public AttemptAnswerValue getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(AttemptAnswerValue studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public Map<String, Object> getCorrectAnswerSnapshot() {
        return correctAnswerSnapshot;
    }

    public void setCorrectAnswerSnapshot(Map<String, Object> correctAnswerSnapshot) {
        this.correctAnswerSnapshot = correctAnswerSnapshot;
    }

    public BigDecimal getScoreAchieved() {
        return scoreAchieved;
    }

    public void setScoreAchieved(BigDecimal scoreAchieved) {
        this.scoreAchieved = scoreAchieved;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }

    public AttemptAnswerStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptAnswerStatus status) {
        this.status = status;
    }
}
