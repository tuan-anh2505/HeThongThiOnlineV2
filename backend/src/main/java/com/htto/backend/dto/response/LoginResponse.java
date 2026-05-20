package com.htto.backend.dto.response;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        AccountResponse user
) {
}
