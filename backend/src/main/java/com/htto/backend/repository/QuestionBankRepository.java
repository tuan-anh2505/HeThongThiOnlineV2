package com.htto.backend.repository;

import com.htto.backend.domain.QuestionBank;
import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionBankRepository extends MongoRepository<QuestionBank, String> {

    Optional<QuestionBank> findByNameAndTeacherId(String name, String teacherId);

    List<QuestionBank> findBySubjectId(String subjectId);

    List<QuestionBank> findByTeacherId(String teacherId);

    List<QuestionBank> findByTeacherIdAndStatus(String teacherId, QuestionBankStatus status);

    List<QuestionBank> findBySubjectIdAndStatus(String subjectId, QuestionBankStatus status);
}
