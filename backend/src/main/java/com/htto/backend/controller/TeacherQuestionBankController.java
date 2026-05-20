package com.htto.backend.controller;

import com.htto.backend.dto.response.QuestionBankResponse;
import com.htto.backend.service.QuestionBankService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/question-banks")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherQuestionBankController {

    private final QuestionBankService questionBankService;

    public TeacherQuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public List<QuestionBankResponse> getMyQuestionBanks(Authentication authentication) {
        return questionBankService.getTeacherQuestionBanks(authentication.getName());
    }
}
