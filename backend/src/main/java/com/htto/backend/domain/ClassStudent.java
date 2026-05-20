package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "class_students")
@CompoundIndex(name = "idx_class_student", def = "{'classId': 1, 'studentId': 1}")
public class ClassStudent extends AuditableDocument {

    @Indexed
    private String classId;

    @Indexed
    private String studentId;

    private Instant joinedAt;

    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

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

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
