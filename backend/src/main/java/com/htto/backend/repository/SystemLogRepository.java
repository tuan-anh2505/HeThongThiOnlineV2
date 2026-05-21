package com.htto.backend.repository;

import com.htto.backend.domain.SystemLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SystemLogRepository extends MongoRepository<SystemLog, String> {

    List<SystemLog> findByAccountId(String accountId);

    List<SystemLog> findByOccurredAtBetween(Instant from, Instant to);
}
