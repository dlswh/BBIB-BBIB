package com.example.backend.domain.user.controller;

import com.example.backend.domain.user.dto.UpdateUserNameRequestDto;
import com.example.backend.domain.user.dto.UserResponseDto;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserResponseDto> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(userDetails.getUsername()));
    }

    @PatchMapping("/name")
    public ResponseEntity<UserResponseDto> updateMyName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserNameRequestDto request) {
        return ResponseEntity.ok(userService.updateMyName(userDetails.getUsername(), request));
    }
}
