package com.moodandmove.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            String email,
            @JsonProperty("is_email_valid")
            Boolean emailValid,
            @JsonProperty("is_email_verified")
            Boolean emailVerified,
            Profile profile
    ) {
    }
    public record Profile(
            String nickname
    ) {
    }
}