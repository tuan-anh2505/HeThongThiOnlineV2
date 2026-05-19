package com.htto.backend.repository;

import com.htto.backend.domain.TeacherProfile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeacherProfileRepository extends MongoRepository<TeacherProfile, String> {

    Optional<TeacherProfile> findByTeacherCode(String teacherCode);

    Optional<TeacherProfile> findByUserId(String userId);
}
