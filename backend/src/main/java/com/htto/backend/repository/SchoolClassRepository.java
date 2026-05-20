package com.htto.backend.repository;

import com.htto.backend.domain.SchoolClass;
import com.htto.backend.domain.DomainEnums.ClassStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SchoolClassRepository extends MongoRepository<SchoolClass, String> {

    Optional<SchoolClass> findByClassCode(String classCode);

    List<SchoolClass> findByTeacherId(String teacherId);

    List<SchoolClass> findByTeacherIdAndStatus(String teacherId, ClassStatus status);
}
