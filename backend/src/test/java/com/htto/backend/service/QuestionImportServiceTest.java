package com.htto.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.htto.backend.dto.response.QuestionImportResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionImportServiceTest {

    @Mock
    private QuestionService questionService;

    @Test
    void doesNotSaveWhenContentHasInvalidFormat() {
        QuestionImportService service = new QuestionImportService(questionService);

        QuestionImportResponse response = service.importTextContent(
                "bank-1",
                "invalid content",
                "teacher"
        );

        assertThat(response.success()).isFalse();
        assertThat(response.importedCount()).isZero();
        assertThat(response.errors()).isNotEmpty();
        verify(questionService, never()).importQuestions("bank-1", List.of(), "teacher");
    }
}
