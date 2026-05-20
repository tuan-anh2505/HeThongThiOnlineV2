package com.htto.backend.repository;

import com.htto.backend.domain.ClassStudent;
import com.htto.backend.domain.DomainEnums.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassStudentRepository extends MongoRepository<ClassStudent, String> {

    Optional<ClassStudent> findByClassIdAndStudentId(String classId, String studentId);

    Optional<ClassStudent> findByClassIdAndStudentIdAndStatus(
            String classId,
            String studentId,
            EnrollmentStatus status
    );

    List<ClassStudent> findByClassId(String classId);

    List<ClassStudent> findByClassIdAndStatus(String classId, EnrollmentStatus status);

    List<ClassStudent> findByStudentId(String studentId);

    List<ClassStudent> findByStudentIdAndStatus(String studentId, EnrollmentStatus status);
}
