package com.moodandmove.user.dto.request;

import com.moodandmove.user.domain.type.AgeGroup;
import com.moodandmove.user.domain.type.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 50자 이하로 입력해주세요.")
        String nickname,

        @NotNull(message = "나이대를 선택해주세요.")
        AgeGroup ageGroup,

        @NotNull(message = "성별을 선택해주세요.")
        Gender gender

) {
}