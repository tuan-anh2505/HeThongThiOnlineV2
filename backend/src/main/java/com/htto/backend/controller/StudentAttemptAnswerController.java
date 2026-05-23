package com.htto.backend.controller;

import com.htto.backend.dto.request.AttemptAnswerSaveRequest;
import com.htto.backend.dto.response.AttemptAnswerSaveResponse;
import com.htto.backend.dto.response.ExamAttemptStatusResponse;
import com.htto.backend.dto.response.SubmitAttemptResponse;
import com.htto.backend.service.AttemptAnswerService;
import com.htto.backend.service.ExamAttemptSubmitService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/attempts")
@PreAuthorize("hasRole('STUDENT')")
public class StudentAttemptAnswerController {

    private final AttemptAnswerService attemptAnswerService;
    private final ExamAttemptSubmitService examAttemptSubmitService;

    public StudentAttemptAnswerController(
            AttemptAnswerService attemptAnswerService,
            ExamAttemptSubmitService examAttemptSubmitService
    ) {
        this.attemptAnswerService = attemptAnswerService;
        this.examAttemptSubmitService = examAttemptSubmitService;
    }

    @PostMapping("/{attemptId}/answers")
    public AttemptAnswerSaveResponse createOrUpdateAnswer(
            @PathVariable String attemptId,
            @Valid @RequestBody AttemptAnswerSaveRequest request,
            Authentication authentication
    ) {
        return attemptAnswerService.saveAnswer(attemptId, null, request, authentication.getName());
    }

    @PutMapping("/{attemptId}/answers/{questionId}")
    public AttemptAnswerSaveResponse updateAnswer(
            @PathVariable String attemptId,
            @PathVariable String questionId,
            @Valid @RequestBody AttemptAnswerSaveRequest request,
            Authentication authentication
    ) {
        return attemptAnswerService.saveAnswer(attemptId, questionId, request, authentication.getName());
    }

    @PostMapping("/{attemptId}/submit")
    public SubmitAttemptResponse submitAttempt(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return examAttemptSubmitService.submitAttempt(attemptId, authentication.getName());
    }

    @GetMapping("/{attemptId}/status")
    public ExamAttemptStatusResponse getAttemptStatus(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return examAttemptSubmitService.getAttemptStatus(attemptId, authentication.getName());
    }

    @PostMapping("/{attemptId}/auto-submit")
    public SubmitAttemptResponse autoSubmitAttempt(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return examAttemptSubmitService.autoSubmitAttempt(attemptId, authentication.getName());
    }
}
