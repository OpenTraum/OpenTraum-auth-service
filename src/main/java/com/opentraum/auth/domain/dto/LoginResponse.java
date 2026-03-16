package com.opentraum.auth.domain.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
