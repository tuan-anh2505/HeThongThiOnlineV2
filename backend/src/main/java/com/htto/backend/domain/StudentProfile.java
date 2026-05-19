package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.AcademicStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "students")
public class StudentProfile extends AuditableDocument {

    @Indexed(unique = true)
    private String studentCode;

    @Indexed(unique = true)
    private String userId;

    @Indexed
    private String primaryClassId;

    @Indexed
    private List<String> classIds = new ArrayList<>();

    private AcademicStatus status = AcademicStatus.ACTIVE;

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrimaryClassId() {
        return primaryClassId;
    }

    public void setPrimaryClassId(String primaryClassId) {
        this.primaryClassId = primaryClassId;
    }

    public List<String> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<String> classIds) {
        this.classIds = classIds;
    }

    public AcademicStatus getStatus() {
        return status;
    }

    public void setStatus(AcademicStatus status) {
        this.status = status;
    }
}
