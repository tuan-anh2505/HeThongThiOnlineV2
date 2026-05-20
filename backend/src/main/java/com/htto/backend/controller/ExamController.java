package com.htto.backend.controller;

import com.htto.backend.domain.DomainEnums.ExamStatus;
import com.htto.backend.domain.DomainEnums.ResultPublishStatus;
import com.htto.backend.dto.request.ExamCreateRequest;
import com.htto.backend.dto.request.ExamQuestionCreateRequest;
import com.htto.backend.dto.request.ExamUpdateRequest;
import com.htto.backend.dto.response.ExamResponse;
import com.htto.backend.service.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<ExamResponse> searchExams(
            @RequestParam(required = false) String examName,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String questionBankId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(required = false) ResultPublishStatus resultStatus,
            Authentication authentication
    ) {
        return examService.searchExams(
                examName,
                classId,
                subjectId,
                questionBankId,
                teacherId,
                status,
                resultStatus,
                authentication.getName()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse createExam(
            @Valid @RequestBody ExamCreateRequest request,
            Authentication authentication
    ) {
        return examService.createExam(request, authentication.getName());
    }

    @GetMapping("/{id}")
    public ExamResponse getExam(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examService.getExam(id, authentication.getName());
    }

    @PutMapping("/{id}")
    public ExamResponse updateExam(
            @PathVariable String id,
            @Valid @RequestBody ExamUpdateRequest request,
            Authentication authentication
    ) {
        return examService.updateExam(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExam(
            @PathVariable String id,
            Authentication authentication
    ) {
        examService.deleteExam(id, authentication.getName());
    }

    @PostMapping("/{id}/questions")
    public ExamResponse addQuestion(
            @PathVariable String id,
            @Valid @RequestBody ExamQuestionCreateRequest request,
            Authentication authentication
    ) {
        return examService.addQuestion(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuestion(
            @PathVariable String id,
            @PathVariable String questionId,
            Authentication authentication
    ) {
        examService.removeQuestion(id, questionId, authentication.getName());
    }

    @PatchMapping("/{id}/publish")
    public ExamResponse publishExam(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examService.publishExam(id, authentication.getName());
    }

    @PatchMapping("/{id}/close")
    public ExamResponse closeExam(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examService.closeExam(id, authentication.getName());
    }

    @PatchMapping("/{id}/cancel")
    public ExamResponse cancelExam(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examService.cancelExam(id, authentication.getName());
    }
}
