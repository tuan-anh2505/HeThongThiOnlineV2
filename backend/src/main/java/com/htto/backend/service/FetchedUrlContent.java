package com.htto.backend.service;

import java.net.URI;

public record FetchedUrlContent(
        URI uri,
        String contentType,
        byte[] content
) {
}
