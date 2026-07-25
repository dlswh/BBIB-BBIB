package com.example.backend.domain.auth.service;

import com.example.backend.domain.auth.dto.LoginRequestDto;
import com.example.backend.domain.auth.dto.LoginResponseDto;
import com.example.backend.domain.auth.dto.ReissueResponseDto;
import com.example.backend.domain.session.dto.SessionEventDto;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.entity.UserSession;
import com.example.backend.domain.user.repository.UserRepository;
import com.example.backend.domain.user.repository.UserSessionRepository;
import com.example.backend.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest) {

        // 1. ID/PW 검증
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 2. 유저 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        // 3. AT/RT 발급
        String accessToken = jwtProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());

        // 4. Redis에 RT 저장
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUsername(),
                refreshToken,
                14, TimeUnit.DAYS
        );

        // 5. 기존 세션 로그아웃 처리 (중복 로그인 차단)
        userSessionRepository.findAllByUserAndLogoutAtIsNull(user)
                .forEach(session -> {
                    session.logout(LocalDateTime.now());
                    messagingTemplate.convertAndSend("/topic/sessions",
                            new SessionEventDto("LOGOUT", session));
                });

        // 6. 접속 세션 DB에 저장
        String ipAddress = httpRequest.getRemoteAddr();
        UserSession session = UserSession.builder()
                .user(user)
                .loginAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();
        userSessionRepository.save(session);

        // 7. WebSocket으로 LOGIN 이벤트 푸시
        messagingTemplate.convertAndSend("/topic/sessions",
                new SessionEventDto("LOGIN", session));

        return new LoginResponseDto(
                accessToken,
                refreshToken,
                user.getUsername(),
                user.getName(),
                user.getRole().name()
        );
    }

    @Transactional
    public void logout(String accessToken, String username) {

        // 1. Redis에서 RT 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);

        // 2. AT 블랙리스트 등록
        long expiration = jwtProvider.getExpiration(accessToken);
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "logout",
                    expiration,
                    TimeUnit.MILLISECONDS
            );
        }

        // 3. 세션 로그아웃 처리
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        userSessionRepository.findAllByUserAndLogoutAtIsNull(user)
                .forEach(session -> {
                    session.logout(LocalDateTime.now());
                    // WebSocket으로 LOGOUT 이벤트 푸시 (여기 추가!)
                    messagingTemplate.convertAndSend("/topic/sessions",
                            new SessionEventDto("LOGOUT", session));
                });
    }

    @Transactional
    public ReissueResponseDto reissue(String refreshToken) {

        // 1. RT 유효성 검증
        if (!jwtProvider.validateToken(refreshToken) || jwtProvider.isAccessToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다");
        }

        // 2. RT에서 username 추출
        String username = jwtProvider.getUsername(refreshToken);

        // 3. Redis에서 저장된 RT 조회
        String storedRefreshToken = redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + username);

        // 4. Redis RT와 요청 RT 비교 (동시접속 차단)
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh Token이 일치하지 않습니다");
        }

        // 5. 유저 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        // 6. 새 AT + 새 RT 발급 (RT Rotation)
        String newAccessToken = jwtProvider.generateAccessToken(username, user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(username);

        // 7. Redis RT 교체
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + username,
                newRefreshToken,
                14, TimeUnit.DAYS
        );

        return new ReissueResponseDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public LoginResponseDto appLogin(LoginRequestDto request, HttpServletRequest httpRequest) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        // ✅ WORKER만 허용
        if (user.getRole() != User.Role.WORKER) {
            throw new RuntimeException("작업자 계정만 앱 로그인이 가능합니다");
        }

        // 이하 기존 login()과 완전히 동일
        String accessToken = jwtProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUsername(),
                refreshToken,
                14, TimeUnit.DAYS
        );

        userSessionRepository.findAllByUserAndLogoutAtIsNull(user)
                .forEach(session -> {
                    session.logout(LocalDateTime.now());
                    messagingTemplate.convertAndSend("/topic/sessions",
                            new SessionEventDto("LOGOUT", session));
                });

        String ipAddress = httpRequest.getRemoteAddr();
        UserSession session = UserSession.builder()
                .user(user)
                .loginAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();
        userSessionRepository.save(session);

        messagingTemplate.convertAndSend("/topic/sessions",
                new SessionEventDto("LOGIN", session));

        return new LoginResponseDto(
                accessToken,
                refreshToken,
                user.getUsername(),
                user.getName(),
                user.getRole().name()
        );
    }
}