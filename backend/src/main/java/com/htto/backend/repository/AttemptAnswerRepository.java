package com.htto.backend.repository;

import com.htto.backend.domain.AttemptAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AttemptAnswerRepository extends MongoRepository<AttemptAnswer, String> {

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(String attemptId, String questionId);

    List<AttemptAnswer> findByAttemptId(String attemptId);
}
