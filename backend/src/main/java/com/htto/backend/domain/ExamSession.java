package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "exam_sessions")
public class ExamSession extends AuditableDocument {

    @Indexed
    private String examId;

    private Instant startAt;

    private Instant endAt;

    private ExamSessionStatus status = ExamSessionStatus.NOT_OPENED;

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public ExamSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ExamSessionStatus status) {
        this.status = status;
    }
}
