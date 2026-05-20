package com.htto.backend.repository;

import com.htto.backend.domain.Question;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionRepository extends MongoRepository<Question, String> {

    Optional<Question> findByQuestionBankIdAndContent(String questionBankId, String content);

    List<Question> findByQuestionBankId(String questionBankId);

    List<Question> findByQuestionBankIdAndStatus(String questionBankId, QuestionStatus status);
}
