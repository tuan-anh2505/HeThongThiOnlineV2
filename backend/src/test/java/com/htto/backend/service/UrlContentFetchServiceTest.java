package com.htto.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class UrlContentFetchServiceTest {

    private final UrlContentFetchService service = new UrlContentFetchService();

    @Test
    void rejectsUrlWithoutScheme() {
        assertThatThrownBy(() -> service.validateUrl("example.com/questions.txt"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("URL scheme is required");
    }

    @Test
    void rejectsDangerousProtocol() {
        assertThatThrownBy(() -> service.validateUrl("file:///tmp/questions.txt"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only http and https URLs are allowed");
    }

    @Test
    void rejectsLocalhostBeforeSendingRequest() {
        assertThatThrownBy(() -> service.fetch("http://localhost/questions.txt"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Localhost URLs are not allowed");
    }
}
