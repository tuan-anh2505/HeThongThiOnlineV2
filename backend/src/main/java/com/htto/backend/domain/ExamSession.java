package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ExamSessionStatus;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "exam_sessions")
public class ExamSession extends AuditableDocument {

    @Indexed
    private String examId;

    private Instant startTime;

    private Instant endTime;

    private ExamSessionStatus status = ExamSessionStatus.NOT_OPEN;

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public ExamSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ExamSessionStatus status) {
        this.status = status;
    }
}
