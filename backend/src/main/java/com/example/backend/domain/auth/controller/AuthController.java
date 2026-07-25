package com.example.backend.domain.auth.controller;

import com.example.backend.domain.auth.dto.LoginRequestDto;
import com.example.backend.domain.auth.dto.LoginResponseDto;
import com.example.backend.domain.auth.dto.ReissueRequestDto;
import com.example.backend.domain.auth.dto.ReissueResponseDto;
import com.example.backend.domain.auth.dto.SwaggerTokenResponseDto;
import com.example.backend.domain.auth.service.AuthService;
import com.example.backend.global.jwt.JwtProvider;
import com.example.backend.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        LoginResponseDto response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/swagger-token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<SwaggerTokenResponseDto> swaggerToken(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest httpRequest) {

        LoginResponseDto response = authService.login(
                new LoginRequestDto(username, password),
                httpRequest
        );

        return ResponseEntity.ok(new SwaggerTokenResponseDto(
                response.getAccessToken(),
                "Bearer",
                response.getRefreshToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {

        String accessToken = jwtProvider.resolveToken(request);
        authService.logout(accessToken, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponseDto> reissue(@RequestBody ReissueRequestDto request) {
        ReissueResponseDto response = authService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/app/login")
    public ResponseEntity<LoginResponseDto> appLogin(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        LoginResponseDto response = authService.appLogin(request, httpRequest);
        return ResponseEntity.ok(response);
    }

}
