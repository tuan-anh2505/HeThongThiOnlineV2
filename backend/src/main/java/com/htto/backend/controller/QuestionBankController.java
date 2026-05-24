package com.htto.backend.controller;

import com.htto.backend.domain.DomainEnums.QuestionBankStatus;
import com.htto.backend.dto.request.QuestionBankCreateRequest;
import com.htto.backend.dto.request.QuestionBankUpdateRequest;
import com.htto.backend.dto.response.QuestionBankResponse;
import com.htto.backend.service.QuestionBankService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/question-banks")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    public QuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public List<QuestionBankResponse> searchQuestionBanks(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(required = false) QuestionBankStatus status,
            Authentication authentication
    ) {
        return questionBankService.searchQuestionBanks(
                name,
                subjectId,
                teacherId,
                status,
                authentication.getName()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionBankResponse createQuestionBank(
            @Valid @RequestBody QuestionBankCreateRequest request,
            Authentication authentication
    ) {
        return questionBankService.createQuestionBank(request, authentication.getName());
    }

    @GetMapping("/{id}")
    public QuestionBankResponse getQuestionBank(
            @PathVariable String id,
            Authentication authentication
    ) {
        return questionBankService.getQuestionBank(id, authentication.getName());
    }

    @PutMapping("/{id}")
    public QuestionBankResponse updateQuestionBank(
            @PathVariable String id,
            @Valid @RequestBody QuestionBankUpdateRequest request,
            Authentication authentication
    ) {
        return questionBankService.updateQuestionBank(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestionBank(
            @PathVariable String id,
            Authentication authentication
    ) {
        questionBankService.deactivateQuestionBank(id, authentication.getName());
    }

    @PatchMapping("/{id}/deactivate")
    public QuestionBankResponse deactivateQuestionBank(
            @PathVariable String id,
            Authentication authentication
    ) {
        return questionBankService.deactivateQuestionBank(id, authentication.getName());
    }

    @PatchMapping("/{id}/activate")
    public QuestionBankResponse activateQuestionBank(
            @PathVariable String id,
            Authentication authentication
    ) {
        return questionBankService.activateQuestionBank(id, authentication.getName());
    }
}
