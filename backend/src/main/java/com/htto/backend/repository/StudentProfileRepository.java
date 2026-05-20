package com.htto.backend.repository;

import com.htto.backend.domain.StudentProfile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudentProfileRepository extends MongoRepository<StudentProfile, String> {

    Optional<StudentProfile> findByStudentCode(String studentCode);

    Optional<StudentProfile> findByAccountId(String accountId);
}
