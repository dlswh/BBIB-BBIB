package com.example.backend.domain.session.dto;

import com.example.backend.domain.user.entity.UserSession;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SessionEventDto {

    private String eventType;  // LOGIN / LOGOUT
    private String username;
    private String name;
    private String role;
    private LocalDateTime loginAt;
    private String ipAddress;

    public SessionEventDto(String eventType, UserSession session) {
        this.eventType = eventType;
        this.username = session.getUser().getUsername();
        this.name = session.getUser().getName();
        this.role = session.getUser().getRole().name();
        this.loginAt = session.getLoginAt();
        this.ipAddress = session.getIpAddress();
    }
}