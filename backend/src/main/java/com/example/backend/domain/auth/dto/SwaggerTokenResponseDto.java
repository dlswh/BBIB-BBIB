package com.example.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SwaggerTokenResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("refresh_token") String refreshToken
) {
}
