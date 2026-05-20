package com.htto.backend.repository;

import com.htto.backend.domain.Account;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByUsername(String username);

    Optional<Account> findByUsernameAndDeletedFalse(String username);

    Optional<Account> findByEmail(String email);

    Optional<Account> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
