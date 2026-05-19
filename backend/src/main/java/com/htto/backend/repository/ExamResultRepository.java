package com.htto.backend.repository;

import com.htto.backend.domain.ExamResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamResultRepository extends MongoRepository<ExamResult, String> {

    Optional<ExamResult> findByExamIdAndStudentId(String examId, String studentId);

    List<ExamResult> findByExamId(String examId);

    List<ExamResult> findByClassId(String classId);
}
