package com.htto.backend.controller;

import com.htto.backend.dto.response.QuestionBankResponse;
import com.htto.backend.service.QuestionBankService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/question-banks")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionBankController {

    private final QuestionBankService questionBankService;

    public AdminQuestionBankController(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    @GetMapping
    public List<QuestionBankResponse> getAllQuestionBanks() {
        return questionBankService.getAdminQuestionBanks();
    }
}
