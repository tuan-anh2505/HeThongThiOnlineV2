package com.htto.backend.repository;

import com.htto.backend.domain.QuestionImportHistory;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionImportHistoryRepository extends MongoRepository<QuestionImportHistory, String> {

    List<QuestionImportHistory> findByQuestionBankIdOrderByImportedAtDesc(String questionBankId);

    List<QuestionImportHistory> findByUserIdOrderByImportedAtDesc(String userId);
}
