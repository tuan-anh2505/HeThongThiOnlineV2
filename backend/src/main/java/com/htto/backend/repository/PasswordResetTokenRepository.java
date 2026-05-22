package com.htto.backend.repository;

import com.htto.backend.domain.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    List<PasswordResetToken> findByEmailAndUsedFalse(String email);
}
