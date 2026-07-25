package com.example.backend.domain.user.repository;

import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // 현재 접속 중인 세션 조회 (logoutAt이 null인 것)
    List<UserSession> findAllByUserAndLogoutAtIsNull(User user);

    // 현재 접속 중인 전체 세션 조회
    List<UserSession> findAllByLogoutAtIsNull();

    // 특정 유저의 전체 접속 기록
    List<UserSession> findAllByUser(User user);

    @Query("SELECT COUNT(DISTINCT s.user) FROM UserSession s WHERE s.loginAt >= :startOfDay AND s.user.role = 'WORKER'")
    long countDistinctWorkerByLoginAtAfter(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("""
        SELECT COUNT(DISTINCT s.user)
        FROM UserSession s
        WHERE s.user.role = 'WORKER'
          AND (s.loginAt >= :startOfDay OR s.logoutAt IS NULL)
        """)
    long countDistinctActiveOrLoggedInToday(@Param("startOfDay") LocalDateTime startOfDay);
}