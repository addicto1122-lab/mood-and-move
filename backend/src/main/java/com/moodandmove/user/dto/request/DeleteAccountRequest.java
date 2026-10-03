package com.moodandmove.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(

        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword

) {
}