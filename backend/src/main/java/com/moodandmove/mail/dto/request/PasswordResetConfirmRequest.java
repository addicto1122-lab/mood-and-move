package com.moodandmove.mail.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(

        @NotBlank(message = "재설정 토큰이 필요합니다.")
        String resetToken,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(
                min = 8,
                max = 50,
                message = "비밀번호는 8자 이상 50자 이하로 입력해주세요."
        )
        String newPassword

) {}
