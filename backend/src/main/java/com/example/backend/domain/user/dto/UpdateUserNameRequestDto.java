package com.example.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateUserNameRequestDto {

    @NotBlank(message = "이름을 입력해주세요")
    @Size(max = 50, message = "이름은 50자 이하로 입력해주세요")
    private String name;
}
