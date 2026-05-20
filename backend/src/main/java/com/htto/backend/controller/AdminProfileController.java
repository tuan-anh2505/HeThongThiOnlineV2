package com.htto.backend.controller;

import com.htto.backend.dto.response.StudentProfileResponse;
import com.htto.backend.dto.response.TeacherProfileResponse;
import com.htto.backend.service.ProfileService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProfileController {

    private final ProfileService profileService;

    public AdminProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/students")
    public List<StudentProfileResponse> searchStudents(
            @RequestParam(required = false) String studentCode,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email
    ) {
        return profileService.searchStudents(studentCode, fullName, email);
    }

    @GetMapping("/students/{id}")
    public StudentProfileResponse getStudent(@PathVariable String id) {
        return profileService.getStudentById(id);
    }

    @GetMapping("/teachers")
    public List<TeacherProfileResponse> searchTeachers(
            @RequestParam(required = false) String teacherCode,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email
    ) {
        return profileService.searchTeachers(teacherCode, fullName, email);
    }

    @GetMapping("/teachers/{id}")
    public TeacherProfileResponse getTeacher(@PathVariable String id) {
        return profileService.getTeacherById(id);
    }
}
