package com.htto.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.htto.backend.domain.Account;
import com.htto.backend.domain.DomainEnums.ImportSourceType;
import com.htto.backend.dto.request.ImportQuestionsFromUrlRequest;
import com.htto.backend.dto.response.ImportQuestionsResultResponse;
import com.htto.backend.dto.response.QuestionImportErrorResponse;
import com.htto.backend.dto.response.QuestionImportResponse;
import com.htto.backend.repository.AccountRepository;
import com.htto.backend.repository.QuestionImportHistoryRepository;
import com.htto.backend.repository.SystemLogRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class QuestionImportFromUrlServiceTest {

    private static final String BANK_ID = "bank-1";
    private static final String USERNAME = "teacher";

    @Mock
    private QuestionImportService questionImportService;

    @Mock
    private UrlContentFetchService urlContentFetchService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private QuestionImportHistoryRepository importHistoryRepository;

    @Mock
    private SystemLogRepository systemLogRepository;

    private QuestionImportFromUrlService service;

    @BeforeEach
    void setUp() {
        service = new QuestionImportFromUrlService(
                questionImportService,
                urlContentFetchService,
                accountRepository,
                importHistoryRepository,
                systemLogRepository
        );
        Account account = new Account();
        account.setId("account-1");
        account.setUsername(USERNAME);
        lenient().when(accountRepository.findByUsernameAndDeletedFalse(USERNAME)).thenReturn(Optional.of(account));
    }

    @Test
    void importsValidTxtLink() {
        String content = """
                [QUESTION]
                TYPE=TRUE_FALSE
                CONTENT=TCP is connection-oriented.
                SCORE=1
                DIFFICULTY=EASY
                CORRECT=true
                """;
        String url = "https://example.com/questions.txt";
        when(urlContentFetchService.fetch(url)).thenReturn(new FetchedUrlContent(
                URI.create(url),
                "text/plain; charset=utf-8",
                content.getBytes(StandardCharsets.UTF_8)
        ));
        when(questionImportService.importTextContent(eq(BANK_ID), eq(content), eq(USERNAME)))
                .thenReturn(new QuestionImportResponse(1, 1, true, List.of(), List.of()));

        ImportQuestionsResultResponse response = service.importFromUrl(
                BANK_ID,
                new ImportQuestionsFromUrlRequest(url, ImportSourceType.AUTO),
                USERNAME
        );

        assertThat(response.success()).isTrue();
        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        verify(questionImportService).validateImportAccess(BANK_ID, USERNAME);
        verify(importHistoryRepository).save(any());
        verify(systemLogRepository).save(any());
    }

    @Test
    void returnsErrorWhenUrlCannotBeFetched() {
        String url = "https://example.com/missing.txt";
        when(urlContentFetchService.fetch(url)).thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot fetch URL content"
        ));

        ImportQuestionsResultResponse response = service.importFromUrl(
                BANK_ID,
                new ImportQuestionsFromUrlRequest(url, ImportSourceType.TXT),
                USERNAME
        );

        assertThat(response.success()).isFalse();
        assertThat(response.importedCount()).isZero();
        assertThat(response.errors()).containsExactly("Cannot fetch URL content");
        verify(questionImportService, never()).importTextContent(any(), any(), any());
        verify(importHistoryRepository).save(any());
        verify(systemLogRepository).save(any());
    }

    @Test
    void returnsParserErrorsWhenContentHasInvalidFormat() {
        String url = "https://example.com/questions.txt";
        when(urlContentFetchService.fetch(url)).thenReturn(new FetchedUrlContent(
                URI.create(url),
                "text/plain",
                "invalid content".getBytes(StandardCharsets.UTF_8)
        ));
        when(questionImportService.importTextContent(eq(BANK_ID), eq("invalid content"), eq(USERNAME)))
                .thenReturn(QuestionImportResponse.failed(
                        0,
                        List.of(new QuestionImportErrorResponse(0, 1, "No [QUESTION] block found"))
                ));

        ImportQuestionsResultResponse response = service.importFromUrl(
                BANK_ID,
                new ImportQuestionsFromUrlRequest(url, ImportSourceType.AUTO),
                USERNAME
        );

        assertThat(response.success()).isFalse();
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.errors().getFirst()).contains("No [QUESTION] block found");
    }

    @Test
    void rejectsImportWhenTeacherCannotAccessQuestionBank() {
        String url = "https://example.com/questions.txt";
        ResponseStatusException forbidden = new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Teacher can only manage own question bank"
        );
        org.mockito.Mockito.doThrow(forbidden)
                .when(questionImportService)
                .validateImportAccess(BANK_ID, USERNAME);

        assertThatThrownBy(() -> service.importFromUrl(
                BANK_ID,
                new ImportQuestionsFromUrlRequest(url, ImportSourceType.AUTO),
                USERNAME
        )).isSameAs(forbidden);

        verify(urlContentFetchService, never()).fetch(any());
        verify(importHistoryRepository, never()).save(any());
        verify(systemLogRepository, never()).save(any());
    }
}
