package com.htto.backend.controller;

import com.htto.backend.domain.DomainEnums.AssignmentStatus;
import com.htto.backend.dto.request.ClassSubjectTeacherCreateRequest;
import com.htto.backend.dto.request.ClassSubjectTeacherUpdateRequest;
import com.htto.backend.dto.response.ClassSubjectTeacherResponse;
import com.htto.backend.service.ClassSubjectTeacherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/class-subject-teachers")
@PreAuthorize("hasRole('ADMIN')")
public class ClassSubjectTeacherController {

    private final ClassSubjectTeacherService assignmentService;

    public ClassSubjectTeacherController(ClassSubjectTeacherService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<ClassSubjectTeacherResponse> searchAssignments(
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String schoolYear,
            @RequestParam(required = false) AssignmentStatus status
    ) {
        return assignmentService.searchAssignments(classId, subjectId, teacherId, semester, schoolYear, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassSubjectTeacherResponse createAssignment(
            @Valid @RequestBody ClassSubjectTeacherCreateRequest request
    ) {
        return assignmentService.createAssignment(request);
    }

    @PutMapping("/{id}")
    public ClassSubjectTeacherResponse updateAssignment(
            @PathVariable String id,
            @Valid @RequestBody ClassSubjectTeacherUpdateRequest request
    ) {
        return assignmentService.updateAssignment(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable String id) {
        assignmentService.deactivateAssignment(id);
    }

    @PatchMapping("/{id}/deactivate")
    public ClassSubjectTeacherResponse deactivateAssignment(@PathVariable String id) {
        return assignmentService.deactivateAssignment(id);
    }

    @PatchMapping("/{id}/activate")
    public ClassSubjectTeacherResponse activateAssignment(@PathVariable String id) {
        return assignmentService.activateAssignment(id);
    }
}
