package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.SubmissionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "submissions")
@CompoundIndex(name = "uk_exam_student_attempt", def = "{'examId': 1, 'studentId': 1, 'attemptNumber': 1}", unique = true)
public class Submission extends AuditableDocument {

    @Indexed
    private String studentId;

    @Indexed
    private String examId;

    @Indexed
    private String sessionId;

    private Instant startedAt;

    private Instant submittedAt;

    private BigDecimal totalScore;

    private int attemptNumber = 1;

    private SubmissionStatus status = SubmissionStatus.IN_PROGRESS;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }
}
