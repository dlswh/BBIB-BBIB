package com.example.backend.domain.user.controller;

import com.example.backend.domain.user.dto.CreateUserRequestDto;
import com.example.backend.domain.user.dto.UserResponseDto;
import com.example.backend.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 계정 생성
    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // 계정 비활성화
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.ok().build();
    }

    // 계정 활성화
    @PatchMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return ResponseEntity.ok().build();
    }

    // 작업자 목록 조회
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getWorkers() {
        return ResponseEntity.ok(userService.getWorkers());
    }
}