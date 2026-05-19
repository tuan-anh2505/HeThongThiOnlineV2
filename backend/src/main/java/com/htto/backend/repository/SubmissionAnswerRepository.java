package com.htto.backend.repository;

import com.htto.backend.domain.SubmissionAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubmissionAnswerRepository extends MongoRepository<SubmissionAnswer, String> {

    Optional<SubmissionAnswer> findBySubmissionIdAndQuestionId(String submissionId, String questionId);

    List<SubmissionAnswer> findBySubmissionId(String submissionId);

    List<SubmissionAnswer> findByExamId(String examId);
}
