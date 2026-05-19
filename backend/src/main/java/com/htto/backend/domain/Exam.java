package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.embedded.ExamQuestionRef;
import com.htto.backend.domain.embedded.ExamSettings;
import com.htto.backend.domain.embedded.QuestionSelectionConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "exams")
@CompoundIndex(name = "idx_exam_subject_teacher", def = "{'subjectId': 1, 'teacherId': 1}")
public class Exam extends AuditableDocument {

    private String title;

    @Indexed
    private List<String> classIds = new ArrayList<>();

    @Indexed
    private String subjectId;

    @Indexed
    private String questionBankId;

    @Indexed
    private String teacherId;

    private int durationMinutes;

    private int maxAttempts = 1;

    private BigDecimal totalScore;

    private ExamSettings settings = new ExamSettings();

    private QuestionSelectionConfig selectionConfig = new QuestionSelectionConfig();

    private List<ExamQuestionRef> questionRefs = new ArrayList<>();

    private ExamStatus status = ExamStatus.DRAFT;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<String> classIds) {
        this.classIds = classIds;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(String questionBankId) {
        this.questionBankId = questionBankId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public ExamSettings getSettings() {
        return settings;
    }

    public void setSettings(ExamSettings settings) {
        this.settings = settings;
    }

    public QuestionSelectionConfig getSelectionConfig() {
        return selectionConfig;
    }

    public void setSelectionConfig(QuestionSelectionConfig selectionConfig) {
        this.selectionConfig = selectionConfig;
    }

    public List<ExamQuestionRef> getQuestionRefs() {
        return questionRefs;
    }

    public void setQuestionRefs(List<ExamQuestionRef> questionRefs) {
        this.questionRefs = questionRefs;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }
}
