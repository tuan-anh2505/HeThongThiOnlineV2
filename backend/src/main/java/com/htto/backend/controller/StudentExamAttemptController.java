package com.htto.backend.controller;

import com.htto.backend.dto.request.StartExamRequest;
import com.htto.backend.dto.response.ExamAttemptResponse;
import com.htto.backend.service.ExamAttemptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamAttemptController {

    private final ExamAttemptService examAttemptService;

    public StudentExamAttemptController(ExamAttemptService examAttemptService) {
        this.examAttemptService = examAttemptService;
    }

    @PostMapping("/exams/{examId}/start")
    public ExamAttemptResponse startExam(
            @PathVariable String examId,
            @RequestBody(required = false) StartExamRequest request,
            Authentication authentication
    ) {
        return examAttemptService.startExam(examId, request, authentication.getName());
    }

    @GetMapping("/attempts/{attemptId}")
    public ExamAttemptResponse getAttempt(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return examAttemptService.getAttempt(attemptId, authentication.getName());
    }
}
