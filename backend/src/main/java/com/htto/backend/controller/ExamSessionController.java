package com.htto.backend.controller;

import com.htto.backend.dto.request.ExamSessionCreateRequest;
import com.htto.backend.dto.request.ExamSessionUpdateRequest;
import com.htto.backend.dto.response.ExamSessionAccessResponse;
import com.htto.backend.dto.response.ExamSessionResponse;
import com.htto.backend.service.ExamSessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }

    @GetMapping("/exams/{examId}/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<ExamSessionResponse> getExamSessions(
            @PathVariable String examId,
            Authentication authentication
    ) {
        return examSessionService.getExamSessions(examId, authentication.getName());
    }

    @PostMapping("/exams/{examId}/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamSessionResponse createSession(
            @PathVariable String examId,
            @Valid @RequestBody ExamSessionCreateRequest request,
            Authentication authentication
    ) {
        return examSessionService.createSession(examId, request, authentication.getName());
    }

    @GetMapping("/exam-sessions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ExamSessionResponse getSession(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examSessionService.getSession(id, authentication.getName());
    }

    @PutMapping("/exam-sessions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ExamSessionResponse updateSession(
            @PathVariable String id,
            @Valid @RequestBody ExamSessionUpdateRequest request,
            Authentication authentication
    ) {
        return examSessionService.updateSession(id, request, authentication.getName());
    }

    @DeleteMapping("/exam-sessions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(
            @PathVariable String id,
            Authentication authentication
    ) {
        examSessionService.deleteSession(id, authentication.getName());
    }

    @PatchMapping("/exam-sessions/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ExamSessionResponse cancelSession(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examSessionService.cancelSession(id, authentication.getName());
    }

    @GetMapping("/exam-sessions/{id}/access")
    @PreAuthorize("hasRole('STUDENT')")
    public ExamSessionAccessResponse checkStudentAccess(
            @PathVariable String id,
            Authentication authentication
    ) {
        return examSessionService.checkStudentAccess(id, authentication.getName());
    }
}
