package com.example.backend.domain.user.repository;

import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAllByRole(User.Role role);
    long countByRoleAndEnabledTrue(User.Role role);
    List<User> findAllByRoleAndEnabled(User.Role role, boolean enabled);
}