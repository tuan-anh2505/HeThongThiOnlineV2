package com.htto.backend.domain;

import java.math.BigDecimal;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "exam_questions")
@CompoundIndexes({
        @CompoundIndex(name = "uk_exam_question", def = "{'examId': 1, 'questionId': 1}", unique = true),
        @CompoundIndex(name = "idx_exam_question_order", def = "{'examId': 1, 'orderIndex': 1}")
})
public class ExamQuestion extends AuditableDocument {

    @Indexed
    private String examId;

    @Indexed
    private String questionId;

    private BigDecimal score;

    private int orderIndex;

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

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
