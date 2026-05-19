package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import java.math.BigDecimal;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "exam_results")
@CompoundIndex(name = "uk_exam_student_result", def = "{'examId': 1, 'studentId': 1}", unique = true)
public class ExamResult extends AuditableDocument {

    @Indexed
    private String examId;

    @Indexed
    private String classId;

    @Indexed
    private String studentId;

    @Indexed
    private String submissionId;

    private BigDecimal score;

    private BigDecimal maxScore;

    private Integer rank;

    private ResultPublishStatus publishStatus = ResultPublishStatus.UNPUBLISHED;

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public ResultPublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(ResultPublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }
}
