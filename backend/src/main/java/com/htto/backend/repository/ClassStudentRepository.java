package com.htto.backend.repository;

import com.htto.backend.domain.ClassStudent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassStudentRepository extends MongoRepository<ClassStudent, String> {

    Optional<ClassStudent> findByClassIdAndStudentId(String classId, String studentId);

    List<ClassStudent> findByClassId(String classId);

    List<ClassStudent> findByStudentId(String studentId);
}
