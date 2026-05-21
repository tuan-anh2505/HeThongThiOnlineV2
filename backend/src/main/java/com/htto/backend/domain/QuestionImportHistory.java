package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ImportSourceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "question_import_histories")
@CompoundIndex(name = "idx_import_history_bank_time", def = "{'questionBankId': 1, 'importedAt': -1}")
public class QuestionImportHistory extends AuditableDocument {

    @Indexed
    private String userId;

    @Indexed
    private String questionBankId;

    private String sourceUrl;

    private ImportSourceType sourceType;

    private boolean success;

    private int importedCount;

    private int failedCount;

    private List<String> errors = new ArrayList<>();

    private Instant importedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuestionBankId() {
        return questionBankId;
    }

    public void setQuestionBankId(String questionBankId) {
        this.questionBankId = questionBankId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public ImportSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ImportSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
