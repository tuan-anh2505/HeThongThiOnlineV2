package com.htto.backend.repository;

import com.htto.backend.domain.Exam;
import com.htto.backend.domain.DomainEnums.ExamStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamRepository extends MongoRepository<Exam, String> {

    Optional<Exam> findByTitleAndTeacherId(String title, String teacherId);

    List<Exam> findByTeacherId(String teacherId);

    List<Exam> findByTeacherIdAndStatus(String teacherId, ExamStatus status);

    List<Exam> findBySubjectId(String subjectId);

    List<Exam> findByQuestionBankId(String questionBankId);
}
