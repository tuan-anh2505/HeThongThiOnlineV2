package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.embedded.ExamAttemptQuestionSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "exam_attempts")
@CompoundIndex(name = "uk_attempt_exam_student_number", def = "{'examId': 1, 'studentId': 1, 'attemptNumber': 1}", unique = true)
public class ExamAttempt extends AuditableDocument {

    @Indexed
    private String studentId;

    @Indexed
    private String examId;

    @Indexed
    private String sessionId;

    private Instant startedAt;

    private Instant submittedAt;

    private Instant deadline;

    private BigDecimal totalScore;

    @Indexed
    private ExamAttemptStatus status = ExamAttemptStatus.IN_PROGRESS;

    private int attemptNumber = 1;

    private List<ExamAttemptQuestionSnapshot> questionSnapshots = new ArrayList<>();

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

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public ExamAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(ExamAttemptStatus status) {
        this.status = status;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public List<ExamAttemptQuestionSnapshot> getQuestionSnapshots() {
        return questionSnapshots;
    }

    public void setQuestionSnapshots(List<ExamAttemptQuestionSnapshot> questionSnapshots) {
        this.questionSnapshots = questionSnapshots;
    }
}
