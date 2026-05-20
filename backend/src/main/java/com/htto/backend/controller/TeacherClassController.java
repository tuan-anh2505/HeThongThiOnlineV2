package com.htto.backend.controller;

import com.htto.backend.dto.response.ClassResponse;
import com.htto.backend.service.ClassService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherClassController {

    private final ClassService classService;

    public TeacherClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public List<ClassResponse> getMyClasses(Authentication authentication) {
        return classService.getTeacherClasses(authentication.getName());
    }
}
