package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.ProfileStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "teachers")
public class TeacherProfile extends AuditableDocument {

    @Indexed(unique = true)
    private String teacherCode;

    @Indexed(unique = true)
    private String accountId;

    @Indexed
    private List<String> classIds = new ArrayList<>();

    @Indexed
    private List<String> subjectIds = new ArrayList<>();

    private ProfileStatus status = ProfileStatus.ACTIVE;

    public String getTeacherCode() {
        return teacherCode;
    }

    public void setTeacherCode(String teacherCode) {
        this.teacherCode = teacherCode;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public List<String> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<String> classIds) {
        this.classIds = classIds;
    }

    public List<String> getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(List<String> subjectIds) {
        this.subjectIds = subjectIds;
    }

    public ProfileStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileStatus status) {
        this.status = status;
    }
}
