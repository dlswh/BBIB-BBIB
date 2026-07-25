package com.example.backend.domain.user.dto;

import com.example.backend.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponseDto {

    private Long id;
    private String username;
    private String name;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.role = user.getRole().name();
        this.enabled = user.isEnabled();
        this.createdAt = user.getCreatedAt();
    }
}