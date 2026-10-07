package com.moodandmove.user.controller;

import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.dto.request.UpdateLocationConsentRequest;
import com.moodandmove.user.dto.response.ConsentPolicyResponse;
import com.moodandmove.user.dto.response.LocationConsentResponse;
import com.moodandmove.user.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    // 회원가입 페이지용 약관 조회
    @GetMapping("/current-location")
    public ResponseEntity<ConsentPolicyResponse>
    getCurrentLocationPolicy() {

        ConsentPolicyResponse response =
                consentService.getCurrentLocationPolicy();

        return ResponseEntity.ok(response);
    }

    // 현재 로그인 사용자의 위치 약관 동의 상태 조회
    @GetMapping("/me/current-location")
    public ResponseEntity<LocationConsentResponse>
    getMyLocationConsent(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        LocationConsentResponse response =
                consentService.getLocationConsent(
                        user.getId()
                );

        return ResponseEntity.ok(response);
    }

    // 현재 로그인 사용자의 위치 약관 동의 상태 변경
    @PatchMapping("/me/current-location")
    public ResponseEntity<Void>
    updateMyLocationConsent(
            Authentication authentication,
            @RequestBody UpdateLocationConsentRequest request
    ) {

        User user =
                (User) authentication.getPrincipal();

        consentService.updateLocationConsent(
                user.getId(),
                request.agreed()
        );

        return ResponseEntity.ok().build();
    }
}