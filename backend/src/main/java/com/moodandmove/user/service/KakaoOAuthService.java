package com.moodandmove.user.service;

import com.moodandmove.user.dto.response.KakaoTokenResponse;
import com.moodandmove.user.dto.response.KakaoUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KakaoOAuthService {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthService(
            @Value("${kakao.oauth.client-id}") String clientId,
            @Value("${kakao.oauth.client-secret}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri}") String redirectUri
    ) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }


    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public String getAccessToken(String code) {
        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            body.add("client_secret", clientSecret);
        }
        KakaoTokenResponse response =
                restClient.post()
                        .uri("https://kauth.kakao.com/oauth/token")
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED
                        )
                        .body(body)
                        .retrieve()
                        .body(KakaoTokenResponse.class);

        if (response == null ||
                response.accessToken() == null) {
            throw new IllegalStateException(
                    "카카오 Access Token 발급에 실패했습니다."
            );
        }

        return response.accessToken();
    }

    public KakaoUserResponse getUser(
            String accessToken
    ) {

        KakaoUserResponse response =
                restClient.get()
                        .uri(
                                "https://kapi.kakao.com/v2/user/me"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(KakaoUserResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "카카오 사용자 정보를 불러오지 못했습니다."
            );
        }

        return response;
    }
}