package com.example.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReissueResponseDto {

    private String accessToken;
    private String refreshToken;
}