package com.htto.backend.controller;

import com.htto.backend.dto.request.AttemptAnswerSaveRequest;
import com.htto.backend.dto.response.AttemptAnswerSaveResponse;
import com.htto.backend.service.AttemptAnswerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    public StudentAttemptAnswerController(AttemptAnswerService attemptAnswerService) {
        this.attemptAnswerService = attemptAnswerService;
    }

    @PostMapping("/{attemptId}/answers")
    public AttemptAnswerSaveResponse createOrUpdateAnswer(
            @PathVariable String attemptId,
            @RequestBody AttemptAnswerSaveRequest request,
            Authentication authentication
    ) {
        return attemptAnswerService.saveAnswer(attemptId, null, request, authentication.getName());
    }

    @PutMapping("/{attemptId}/answers/{questionId}")
    public AttemptAnswerSaveResponse updateAnswer(
            @PathVariable String attemptId,
            @PathVariable String questionId,
            @RequestBody AttemptAnswerSaveRequest request,
            Authentication authentication
    ) {
        return attemptAnswerService.saveAnswer(attemptId, questionId, request, authentication.getName());
    }
}
