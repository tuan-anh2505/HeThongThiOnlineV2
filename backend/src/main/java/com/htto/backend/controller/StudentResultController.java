package com.htto.backend.controller;

import com.htto.backend.dto.response.StudentAttemptResultResponse;
import com.htto.backend.dto.response.StudentAttemptReviewResponse;
import com.htto.backend.dto.response.StudentResultSummaryResponse;
import com.htto.backend.service.StudentResultService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentResultController {

    private final StudentResultService studentResultService;

    public StudentResultController(StudentResultService studentResultService) {
        this.studentResultService = studentResultService;
    }

    @GetMapping("/attempts/{attemptId}/result")
    public StudentAttemptResultResponse getAttemptResult(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return studentResultService.getAttemptResult(attemptId, authentication.getName());
    }

    @GetMapping("/attempts/{attemptId}/review")
    public StudentAttemptReviewResponse getAttemptReview(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return studentResultService.getAttemptReview(attemptId, authentication.getName());
    }

    @GetMapping("/results")
    public List<StudentResultSummaryResponse> getMyResults(Authentication authentication) {
        return studentResultService.getMyResults(authentication.getName());
    }
}
