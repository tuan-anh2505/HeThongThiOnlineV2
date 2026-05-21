package com.htto.backend.repository;

import com.htto.backend.domain.DomainEnums.ExamAttemptStatus;
import com.htto.backend.domain.ExamAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamAttemptRepository extends MongoRepository<ExamAttempt, String> {

    List<ExamAttempt> findByExamId(String examId);

    List<ExamAttempt> findByExamIdAndStudentIdOrderByAttemptNumberAsc(String examId, String studentId);

    List<ExamAttempt> findByStudentId(String studentId);

    Optional<ExamAttempt> findByIdAndStudentId(String id, String studentId);

    Optional<ExamAttempt> findFirstByExamIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
            String examId,
            String studentId,
            ExamAttemptStatus status
    );
}
