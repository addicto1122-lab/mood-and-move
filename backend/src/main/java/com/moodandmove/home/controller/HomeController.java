package com.moodandmove.home.controller;

import com.moodandmove.home.dto.response.HomeResponse;
import com.moodandmove.home.service.HomeService;
import com.moodandmove.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ResponseEntity<HomeResponse> getHome(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        HomeResponse response = homeService.getHome(user.getId());

        return ResponseEntity.ok(response);
    }
}