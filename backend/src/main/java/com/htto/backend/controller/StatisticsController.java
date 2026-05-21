package com.htto.backend.controller;

import com.htto.backend.dto.response.ClassStatisticsResponse;
import com.htto.backend.dto.response.ExamStatisticsResponse;
import com.htto.backend.dto.response.SubjectStatisticsResponse;
import com.htto.backend.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/api/teacher/statistics/exams/{examId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ExamStatisticsResponse getTeacherExamStatistics(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return statisticsService.getExamStatistics(examId, authentication.getName());
    }

    @GetMapping("/api/teacher/statistics/classes/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ClassStatisticsResponse getTeacherClassStatistics(
            @PathVariable String classId,
            Authentication authentication
    ) {
        return statisticsService.getClassStatistics(classId, authentication.getName());
    }

    @GetMapping("/api/teacher/statistics/subjects/{subjectId}")
    @PreAuthorize("hasRole('TEACHER')")
    public SubjectStatisticsResponse getTeacherSubjectStatistics(
            @PathVariable String subjectId,
            Authentication authentication
    ) {
        return statisticsService.getSubjectStatistics(subjectId, authentication.getName());
    }

    @GetMapping("/api/admin/statistics/exams/{examId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ExamStatisticsResponse getAdminExamStatistics(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return statisticsService.getExamStatistics(examId, authentication.getName());
    }

    @GetMapping("/api/admin/statistics/classes/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassStatisticsResponse getAdminClassStatistics(
            @PathVariable String classId,
            Authentication authentication
    ) {
        return statisticsService.getClassStatistics(classId, authentication.getName());
    }

    @GetMapping("/api/admin/statistics/subjects/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectStatisticsResponse getAdminSubjectStatistics(
            @PathVariable String subjectId,
            Authentication authentication
    ) {
        return statisticsService.getSubjectStatistics(subjectId, authentication.getName());
    }
}
