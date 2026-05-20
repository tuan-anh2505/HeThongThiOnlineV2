package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.SubjectStatus;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subjects")
public class Subject extends AuditableDocument {

    @Indexed(unique = true)
    private String subjectCode;

    private String subjectName;

    private String description;

    private SubjectStatus status = SubjectStatus.ACTIVE;

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SubjectStatus getStatus() {
        return status;
    }

    public void setStatus(SubjectStatus status) {
        this.status = status;
    }
}
