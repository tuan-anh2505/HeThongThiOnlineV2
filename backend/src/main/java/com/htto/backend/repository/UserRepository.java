package com.htto.backend.repository;

import com.htto.backend.domain.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);
}
