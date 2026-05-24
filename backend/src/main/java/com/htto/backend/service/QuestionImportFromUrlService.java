package com.htto.backend.service;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.DomainEnums.ImportSourceType;
import com.htto.backend.domain.QuestionImportHistory;
import com.htto.backend.dto.request.ImportQuestionsFromUrlRequest;
import com.htto.backend.dto.response.ImportQuestionsResultResponse;
import com.htto.backend.dto.response.QuestionImportErrorResponse;
import com.htto.backend.dto.response.QuestionImportResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.QuestionImportHistoryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionImportFromUrlService {

    private static final String ACTION_IMPORT_FROM_URL = "IMPORT_QUESTIONS_FROM_URL";
    private static final String TARGET_QUESTION_BANK = "QUESTION_BANK";

    private final QuestionImportService questionImportService;
    private final UrlContentFetchService urlContentFetchService;
    private final QuestionTextExtractionService questionTextExtractionService;
    private final AccountRepository accountRepository;
    private final QuestionImportHistoryRepository importHistoryRepository;
    private final SystemLogService systemLogService;

    public QuestionImportFromUrlService(
            QuestionImportService questionImportService,
            UrlContentFetchService urlContentFetchService,
            QuestionTextExtractionService questionTextExtractionService,
            AccountRepository accountRepository,
            QuestionImportHistoryRepository importHistoryRepository,
            SystemLogService systemLogService
    ) {
        this.questionImportService = questionImportService;
        this.urlContentFetchService = urlContentFetchService;
        this.questionTextExtractionService = questionTextExtractionService;
        this.accountRepository = accountRepository;
        this.importHistoryRepository = importHistoryRepository;
        this.systemLogService = systemLogService;
    }

    public ImportQuestionsResultResponse importFromUrl(
            String questionBankId,
            ImportQuestionsFromUrlRequest request,
            String username
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import request is required");
        }

        questionImportService.validateImportAccess(questionBankId, username);
        Account account = getCurrentAccount(username);
        ImportSourceType requestedType = request.sourceType() == null ? ImportSourceType.AUTO : request.sourceType();
        ImportSourceType resolvedType = requestedType;
        ImportQuestionsResultResponse result;

        try {
            FetchedUrlContent fetchedContent = urlContentFetchService.fetch(request.url());
            ExtractedQuestionContent extractedContent = questionTextExtractionService.extractFromUrl(
                    fetchedContent,
                    requestedType
            );
            resolvedType = extractedContent.sourceType();
            QuestionImportResponse importResponse = questionImportService.importTextContent(
                    questionBankId,
                    extractedContent.text(),
                    username
            );
            result = toResultResponse(importResponse);
        } catch (ResponseStatusException ex) {
            result = ImportQuestionsResultResponse.failed(List.of(reasonOf(ex)));
        }

        saveHistoryAndLog(account, questionBankId, request.url(), resolvedType, result);
        return result;
    }

    private ImportQuestionsResultResponse toResultResponse(QuestionImportResponse importResponse) {
        if (importResponse.success()) {
            return ImportQuestionsResultResponse.success(importResponse.importedCount());
        }

        List<String> errors = importResponse.errors()
                .stream()
                .map(this::formatError)
                .toList();
        return ImportQuestionsResultResponse.failed(errors);
    }

    private String formatError(QuestionImportErrorResponse error) {
        List<String> parts = new ArrayList<>();
        if (error.questionIndex() > 0) {
            parts.add("questionIndex=" + error.questionIndex());
        }
        if (error.lineNumber() > 0) {
            parts.add("lineNumber=" + error.lineNumber());
        }
        parts.add(error.message());
        return String.join(", ", parts);
    }

    private void saveHistoryAndLog(
            Account account,
            String questionBankId,
            String sourceUrl,
            ImportSourceType sourceType,
            ImportQuestionsResultResponse result
    ) {
        Instant now = Instant.now();
        List<String> errors = result.errors() == null ? List.of() : result.errors();

        QuestionImportHistory history = new QuestionImportHistory();
        history.setUserId(account.getId());
        history.setQuestionBankId(questionBankId);
        history.setSourceUrl(sourceUrl);
        history.setSourceType(sourceType);
        history.setSuccess(result.success());
        history.setImportedCount(result.importedCount());
        history.setFailedCount(result.failedCount());
        history.setErrors(errors);
        history.setImportedAt(now);
        importHistoryRepository.save(history);

        systemLogService.log(
                account.getId(),
                ACTION_IMPORT_FROM_URL,
                TARGET_QUESTION_BANK,
                questionBankId,
                buildLogDetail(sourceUrl, sourceType, result)
        );
    }

    private String buildLogDetail(
            String sourceUrl,
            ImportSourceType sourceType,
            ImportQuestionsResultResponse result
    ) {
        return "sourceUrl=" + sourceUrl
                + ", sourceType=" + sourceType
                + ", success=" + result.success()
                + ", importedCount=" + result.importedCount()
                + ", failedCount=" + result.failedCount();
    }

    private String reasonOf(ResponseStatusException ex) {
        return ex.getReason() == null ? "Import from URL failed" : ex.getReason();
    }

    private Account getCurrentAccount(String username) {
        return accountRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
