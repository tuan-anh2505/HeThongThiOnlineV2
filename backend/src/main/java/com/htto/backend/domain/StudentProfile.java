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
    private String accountId;

    @Indexed
    private String mainClassId;

    @Indexed
    private List<String> classIds = new ArrayList<>();

    private AcademicStatus status = AcademicStatus.ACTIVE;

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getMainClassId() {
        return mainClassId;
    }

    public void setMainClassId(String mainClassId) {
        this.mainClassId = mainClassId;
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
