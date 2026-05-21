package com.htto.backend.controller;

import com.htto.backend.dto.response.StudentExamDetailResponse;
import com.htto.backend.dto.response.StudentExamListResponse;
import com.htto.backend.service.StudentExamService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/exams")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

    private final StudentExamService studentExamService;

    public StudentExamController(StudentExamService studentExamService) {
        this.studentExamService = studentExamService;
    }

    @GetMapping
    public List<StudentExamListResponse> getMyExams(Authentication authentication) {
        return studentExamService.getStudentExams(authentication.getName());
    }

    @GetMapping("/{examId}")
    public StudentExamDetailResponse getMyExamDetail(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return studentExamService.getStudentExamDetail(examId, authentication.getName());
    }
}
