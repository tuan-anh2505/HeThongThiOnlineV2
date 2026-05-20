package com.htto.backend.repository;

import com.htto.backend.domain.ExamQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamQuestionRepository extends MongoRepository<ExamQuestion, String> {

    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(String examId);

    Optional<ExamQuestion> findByExamIdAndQuestionId(String examId, String questionId);

    boolean existsByExamIdAndQuestionId(String examId, String questionId);

    void deleteByExamIdAndQuestionId(String examId, String questionId);
}
