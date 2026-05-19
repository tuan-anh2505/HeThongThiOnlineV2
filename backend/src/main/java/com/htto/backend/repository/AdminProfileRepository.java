package com.htto.backend.repository;

import com.htto.backend.domain.AdminProfile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdminProfileRepository extends MongoRepository<AdminProfile, String> {

    Optional<AdminProfile> findByAdminCode(String adminCode);

    Optional<AdminProfile> findByUserId(String userId);
}
