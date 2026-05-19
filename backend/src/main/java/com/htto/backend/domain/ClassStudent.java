package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import java.time.Instant;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "class_students")
@CompoundIndex(name = "uk_class_student", def = "{'classId': 1, 'studentId': 1}", unique = true)
public class ClassStudent extends AuditableDocument {

    @Indexed
    private String classId;

    @Indexed
    private String studentId;

    private Instant addedAt;

    private EnrollmentStatus status = EnrollmentStatus.STUDYING;

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

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
