package com.moodandmove.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse (
        @JsonProperty("access_token")
        String accessToken
){
}
