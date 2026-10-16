
package com.moodandmove.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 401: 인증되지 않은 사용자
    @Test
    void unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("HTTP_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."));
    }

    // 404: 존재하지 않는 데이터
    @Test
    void notFound_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HTTP_ERROR"));
    }

    // 409: 요청 데이터 충돌
    @Test
    void conflict_returns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HTTP_ERROR"));
    }

    @RestController
    public static class TestController {

        @GetMapping("/test/unauthorized")
        public void unauthorized() {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "데이터를 찾을 수 없습니다."
            );
        }

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 처리된 요청입니다."
            );
        }
    }
}
