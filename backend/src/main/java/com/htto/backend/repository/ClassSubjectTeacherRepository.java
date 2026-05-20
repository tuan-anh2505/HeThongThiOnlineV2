package com.htto.backend.repository;

import com.htto.backend.domain.ClassSubjectTeacher;
import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassSubjectTeacherRepository extends MongoRepository<ClassSubjectTeacher, String> {

    Optional<ClassSubjectTeacher> findByClassIdAndSubjectIdAndTeacherIdAndStatus(
            String classId,
            String subjectId,
            String teacherId,
            AssignmentStatus status
    );

    List<ClassSubjectTeacher> findByClassIdInAndStatus(Collection<String> classIds, AssignmentStatus status);

    List<ClassSubjectTeacher> findByTeacherIdAndStatus(String teacherId, AssignmentStatus status);

    List<ClassSubjectTeacher> findBySubjectIdAndStatus(String subjectId, AssignmentStatus status);
}
