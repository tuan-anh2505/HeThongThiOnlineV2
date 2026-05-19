package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.embedded.AnswerDefinition;
import java.math.BigDecimal;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "questions")
@CompoundIndex(name = "idx_bank_type_difficulty", def = "{'questionBankId': 1, 'type': 1, 'difficulty': 1}")
public class Question extends AuditableDocument {

    @Indexed
    private String questionBankId;

    private QuestionType type;

    private String content;

    private BigDecimal point;

    private Difficulty difficulty;

    private String topic;

    private QuestionStatus status = QuestionStatus.ACTIVE;

    private AnswerDefinition answerDefinition;

    public String getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(String questionBankId) {
        this.questionBankId = questionBankId;
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

    public BigDecimal getPoint() {
        return point;
    }

    public void setPoint(BigDecimal point) {
        this.point = point;
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

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public AnswerDefinition getAnswerDefinition() {
        return answerDefinition;
    }

    public void setAnswerDefinition(AnswerDefinition answerDefinition) {
        this.answerDefinition = answerDefinition;
    }
}
