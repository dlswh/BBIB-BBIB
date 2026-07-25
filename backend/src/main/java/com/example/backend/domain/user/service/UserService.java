package com.example.backend.domain.user.service;

import com.example.backend.domain.user.dto.CreateUserRequestDto;
import com.example.backend.domain.user.dto.UpdateUserNameRequestDto;
import com.example.backend.domain.user.dto.UserResponseDto;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 계정 생성 (ADMIN 전용)
    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {

        // 중복 username 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 아이디입니다");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .build();

        return new UserResponseDto(userRepository.save(user));
    }

    // 계정 비활성화 (ADMIN 전용)
    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("ADMIN 계정은 비활성화할 수 없습니다");
        }

        user.disable();
    }

    // 계정 활성화 (ADMIN 전용)
    @Transactional
    public void enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("ADMIN 계정은 변경할 수 없습니다");
        }

        user.enable();
    }

    // 작업자 목록 조회 (ADMIN 전용)
    @Transactional(readOnly = true)
    public List<UserResponseDto> getWorkers() {
        return userRepository.findAllByRole(User.Role.WORKER)
                .stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDto getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        return new UserResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateMyName(String username, UpdateUserNameRequestDto request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));

        user.updateName(request.getName());
        return new UserResponseDto(user);
    }

    @Bean
    public CommandLineRunner initAdmin() {

        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("1234"))
                        .name("관리자")
                        .role(User.Role.ADMIN)
                        .build();

                userRepository.save(admin);

                System.out.println("기본 admin 계정 생성 완료");
            }
        };
    }
}
