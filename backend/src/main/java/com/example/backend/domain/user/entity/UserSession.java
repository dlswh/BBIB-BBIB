package com.example.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime loginAt;

    private LocalDateTime logoutAt;

    @Column(length = 50)
    private String ipAddress;

    @Builder
    public UserSession(User user, LocalDateTime loginAt, String ipAddress) {
        this.user = user;
        this.loginAt = loginAt;
        this.ipAddress = ipAddress;
    }

    // 로그아웃 시 호출
    public void logout(LocalDateTime logoutAt) {
        this.logoutAt = logoutAt;
    }
}