package com.htto.backend.controller;

import com.htto.backend.domain.DomainEnums.ClassStatus;
import com.htto.backend.dto.request.AddStudentToClassRequest;
import com.htto.backend.dto.request.ClassCreateRequest;
import com.htto.backend.dto.request.ClassUpdateRequest;
import com.htto.backend.dto.response.ClassResponse;
import com.htto.backend.dto.response.ClassStudentResponse;
import com.htto.backend.service.ClassService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ClassResponse> searchClasses(
            @RequestParam(required = false) String classCode,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) ClassStatus status
    ) {
        return classService.searchClasses(classCode, className, teacherId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClassResponse createClass(@Valid @RequestBody ClassCreateRequest request) {
        return classService.createClass(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassResponse getClass(@PathVariable String id) {
        return classService.getClass(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassResponse updateClass(
            @PathVariable String id,
            @Valid @RequestBody ClassUpdateRequest request
    ) {
        return classService.updateClass(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteClass(@PathVariable String id) {
        classService.deleteClass(id);
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<ClassStudentResponse> getStudents(
            @PathVariable String id,
            Authentication authentication
    ) {
        return classService.getStudents(id, authentication.getName());
    }

    @PostMapping("/{id}/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ClassStudentResponse addStudent(
            @PathVariable String id,
            @Valid @RequestBody AddStudentToClassRequest request,
            Authentication authentication
    ) {
        return classService.addStudent(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void removeStudent(
            @PathVariable String id,
            @PathVariable String studentId,
            Authentication authentication
    ) {
        classService.removeStudent(id, studentId, authentication.getName());
    }
}
