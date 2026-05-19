package com.htto.backend.repository;

import com.htto.backend.domain.TeachingAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeachingAssignmentRepository extends MongoRepository<TeachingAssignment, String> {

    Optional<TeachingAssignment> findByClassIdAndSubjectIdAndTeacherId(
            String classId,
            String subjectId,
            String teacherId
    );

    List<TeachingAssignment> findByTeacherId(String teacherId);
}
