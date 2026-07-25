package com.example.backend.domain.user.dto;

import com.example.backend.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class CreateUserRequestDto {

    @NotBlank(message = "아이디를 입력해주세요")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

    @NotBlank(message = "이름을 입력해주세요")
    private String name;

    @NotNull(message = "역할을 선택해주세요")
    private User.Role role;
}