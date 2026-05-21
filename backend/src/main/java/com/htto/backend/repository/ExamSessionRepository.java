package com.htto.backend.repository;

import com.htto.backend.domain.ExamSession;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamSessionRepository extends MongoRepository<ExamSession, String> {

    List<ExamSession> findByExamId(String examId);

    List<ExamSession> findByExamIdOrderByStartTimeAsc(String examId);
}
