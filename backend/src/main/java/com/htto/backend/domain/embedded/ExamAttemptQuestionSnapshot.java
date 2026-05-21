package com.htto.backend.domain.embedded;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExamAttemptQuestionSnapshot {

    private String questionId;
    private QuestionType type;
    private String content;
    private BigDecimal score;
    private Difficulty difficulty;
    private String topic;
    private int orderIndex;
    private List<ExamAttemptOptionSnapshot> options = new ArrayList<>();
    private ExamAttemptFillBlankRuleSnapshot fillBlankRule;
    private ExamAttemptMatchingSnapshot matching;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public List<ExamAttemptOptionSnapshot> getOptions() {
        return options;
    }

    public void setOptions(List<ExamAttemptOptionSnapshot> options) {
        this.options = options;
    }

    public ExamAttemptFillBlankRuleSnapshot getFillBlankRule() {
        return fillBlankRule;
    }

    public void setFillBlankRule(ExamAttemptFillBlankRuleSnapshot fillBlankRule) {
        this.fillBlankRule = fillBlankRule;
    }

    public ExamAttemptMatchingSnapshot getMatching() {
        return matching;
    }

    public void setMatching(ExamAttemptMatchingSnapshot matching) {
        this.matching = matching;
    }
}
