package com.example.backend.domain.auth.dto;

import lombok.Getter;

@Getter
public class ReissueRequestDto {
    private String refreshToken;
}