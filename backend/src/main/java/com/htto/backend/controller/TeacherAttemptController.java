package com.htto.backend.controller;

import com.htto.backend.dto.response.ExamResultStatusResponse;
import com.htto.backend.dto.response.TeacherAttemptDetailResponse;
import com.htto.backend.dto.response.TeacherAttemptSummaryResponse;
import com.htto.backend.service.TeacherAttemptService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherAttemptController {

    private final TeacherAttemptService teacherAttemptService;

    public TeacherAttemptController(TeacherAttemptService teacherAttemptService) {
        this.teacherAttemptService = teacherAttemptService;
    }

    @GetMapping("/exams/{examId}/attempts")
    public List<TeacherAttemptSummaryResponse> getExamAttempts(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return teacherAttemptService.getExamAttempts(examId, authentication.getName());
    }

    @GetMapping("/attempts/{attemptId}")
    public TeacherAttemptDetailResponse getAttemptDetail(
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return teacherAttemptService.getAttemptDetail(attemptId, authentication.getName());
    }

    @PatchMapping("/exams/{examId}/publish-results")
    public ExamResultStatusResponse publishResults(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return teacherAttemptService.publishResults(examId, authentication.getName());
    }

    @PatchMapping("/exams/{examId}/hide-results")
    public ExamResultStatusResponse hideResults(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return teacherAttemptService.hideResults(examId, authentication.getName());
    }
}
