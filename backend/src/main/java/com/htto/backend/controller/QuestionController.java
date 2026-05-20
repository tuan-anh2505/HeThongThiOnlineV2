package com.htto.backend.controller;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionStatus;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.dto.request.QuestionCreateRequest;
import com.htto.backend.dto.request.QuestionUpdateRequest;
import com.htto.backend.dto.response.QuestionResponse;
import com.htto.backend.service.QuestionService;
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
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/question-banks/{bankId}/questions")
    public List<QuestionResponse> searchQuestions(
            @PathVariable String bankId,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) QuestionStatus status,
            Authentication authentication
    ) {
        return questionService.searchQuestions(
                bankId,
                type,
                content,
                difficulty,
                topic,
                status,
                authentication.getName()
        );
    }

    @PostMapping("/question-banks/{bankId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(
            @PathVariable String bankId,
            @Valid @RequestBody QuestionCreateRequest request,
            Authentication authentication
    ) {
        return questionService.createQuestion(bankId, request, authentication.getName());
    }

    @GetMapping("/questions/{id}")
    public QuestionResponse getQuestion(
            @PathVariable String id,
            Authentication authentication
    ) {
        return questionService.getQuestion(id, authentication.getName());
    }

    @PutMapping("/questions/{id}")
    public QuestionResponse updateQuestion(
            @PathVariable String id,
            @Valid @RequestBody QuestionUpdateRequest request,
            Authentication authentication
    ) {
        return questionService.updateQuestion(id, request, authentication.getName());
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(
            @PathVariable String id,
            Authentication authentication
    ) {
        questionService.deleteQuestion(id, authentication.getName());
    }
}
