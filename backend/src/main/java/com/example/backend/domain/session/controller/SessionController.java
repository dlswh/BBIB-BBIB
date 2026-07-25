package com.example.backend.domain.session.controller;

import com.example.backend.domain.user.dto.UserResponseDto;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.entity.UserSession;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.domain.user.repository.UserSessionRepository;
import com.example.backend.domain.session.dto.SessionEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    // 현재 접속 중인 세션 목록 조회
    @GetMapping
    public ResponseEntity<List<SessionEventDto>> getActiveSessions() {
        List<SessionEventDto> sessions = userSessionRepository.findAllByLogoutAtIsNull()
                .stream()
                .map(session -> new SessionEventDto("LOGIN", session))
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    // 금일 총 접속자 수
    @GetMapping("/today-count")
    public ResponseEntity<Long> getTodayCount() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return ResponseEntity.ok(userSessionRepository.countDistinctActiveOrLoggedInToday(startOfDay));
    }

    // 전체 등록 작업자 수
    @GetMapping("/total-workers")
    public ResponseEntity<Long> getTotalWorkers() {
        return ResponseEntity.ok(userRepository.countByRoleAndEnabledTrue(User.Role.WORKER));
    }
}