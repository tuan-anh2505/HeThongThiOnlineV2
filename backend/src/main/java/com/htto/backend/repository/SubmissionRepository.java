package com.htto.backend.repository;

import com.htto.backend.domain.Submission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Optional<Submission> findByExamIdAndStudentIdAndAttemptNumber(String examId, String studentId, int attemptNumber);

    List<Submission> findByExamIdAndStudentId(String examId, String studentId);

    List<Submission> findByExamId(String examId);

    List<Submission> findByStudentId(String studentId);
}
