package com.htto.backend.repository;

import com.htto.backend.domain.Subject;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubjectRepository extends MongoRepository<Subject, String> {

    Optional<Subject> findBySubjectCode(String subjectCode);
}
