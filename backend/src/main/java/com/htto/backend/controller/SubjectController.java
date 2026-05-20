package com.htto.backend.controller;

import com.htto.backend.domain.DomainEnums.SubjectStatus;
import com.htto.backend.dto.request.SubjectCreateRequest;
import com.htto.backend.dto.request.SubjectUpdateRequest;
import com.htto.backend.dto.response.SubjectResponse;
import com.htto.backend.service.SubjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/subjects")
@PreAuthorize("hasRole('ADMIN')")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public List<SubjectResponse> searchSubjects(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) SubjectStatus status
    ) {
        return subjectService.searchSubjects(subjectCode, subjectName, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse createSubject(@Valid @RequestBody SubjectCreateRequest request) {
        return subjectService.createSubject(request);
    }

    @GetMapping("/{id}")
    public SubjectResponse getSubject(@PathVariable String id) {
        return subjectService.getSubject(id);
    }

    @PutMapping("/{id}")
    public SubjectResponse updateSubject(
            @PathVariable String id,
            @Valid @RequestBody SubjectUpdateRequest request
    ) {
        return subjectService.updateSubject(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@PathVariable String id) {
        subjectService.deleteSubject(id);
    }
}
