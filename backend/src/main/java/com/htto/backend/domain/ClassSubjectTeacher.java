package com.htto.backend.domain;

import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "class_subject_teachers")
@CompoundIndex(name = "idx_class_subject_teacher", def = "{'classId': 1, 'subjectId': 1, 'teacherId': 1}")
public class ClassSubjectTeacher extends AuditableDocument {

    @Indexed
    private String classId;

    @Indexed
    private String subjectId;

    @Indexed
    private String teacherId;

    private String semester;

    private String schoolYear;

    @Indexed
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }
}
