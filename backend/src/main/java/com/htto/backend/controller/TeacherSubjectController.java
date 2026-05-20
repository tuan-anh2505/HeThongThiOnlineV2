package com.htto.backend.controller;

import com.htto.backend.dto.response.ClassSubjectTeacherResponse;
import com.htto.backend.service.ClassSubjectTeacherService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/subjects")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherSubjectController {

    private final ClassSubjectTeacherService assignmentService;

    public TeacherSubjectController(ClassSubjectTeacherService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<ClassSubjectTeacherResponse> getMySubjects(Authentication authentication) {
        return assignmentService.getTeacherSubjects(authentication.getName());
    }
}
