package com.moodandmove.user.service;

import com.moodandmove.common.security.JwtProvider;
import com.moodandmove.common.security.RefreshTokenHasher;
import com.moodandmove.user.domain.entity.*;
import com.moodandmove.user.domain.type.ConsentType;
import com.moodandmove.user.domain.type.SocialProvider;
import com.moodandmove.user.dto.request.LoginRequest;
import com.moodandmove.user.dto.request.SignupRequest;
import com.moodandmove.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserWithdrawalRequestRepository userWithdrawalRequestRepository;
    private final ConsentPolicyRepository consentPolicyRepository;
    private final UserConsentRepository userConsentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtProvider jwtProvider;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public void signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }


        ConsentPolicy locationPolicy =
                consentPolicyRepository
                        .findById(request.locationPolicyId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 약관입니다."
                                )
                        );

        /*
         * CURRENT_LOCATION 약관인지 확인
         */
        if (locationPolicy.getConsentType()
                != ConsentType.CURRENT_LOCATION) {

            throw new IllegalArgumentException(
                    "올바르지 않은 위치 이용 약관입니다."
            );
        }


        if (!locationPolicy.isActive()) {
            throw new IllegalArgumentException(
                    "현재 사용할 수 없는 약관입니다."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        User user = User.create(
                request.email(),
                encodedPassword,
                request.nickname()
        );

        userRepository.save(user);


        UserConsent userConsent =
                UserConsent.create(
                        user,
                        locationPolicy,
                        request.locationConsent()
                );

        userConsentRepository.save(userConsent);
    }

    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email){
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    @Transactional
    public void logout(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        refreshTokenRepository.deleteAllByUser_Id(userId);

        user.increaseTokenVersion();
    }
    @Transactional(readOnly = true)
    public boolean isWithdrawalPending(Long userId) {
        return userWithdrawalRequestRepository.existsByUser_Id(userId);
    }

    @Transactional
    public User recoverAccount(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        if (!userWithdrawalRequestRepository.existsByUser_Id(user.getId())) {
            throw new IllegalArgumentException(
                    "탈퇴 신청 상태가 아닙니다."
            );
        }

        userWithdrawalRequestRepository.deleteByUser_Id(
                user.getId()
        );

        return user;
    }

    @Transactional
    public void saveRefreshToken(
            User user,
            String rawRefreshToken,
            LocalDateTime expiresAt
    ) {

        String tokenHash =
                refreshTokenHasher.hash(
                        rawRefreshToken
                );

        refreshTokenRepository
                .deleteAllByUser_Id(
                        user.getId()
                );


        refreshTokenRepository.flush();

        RefreshToken refreshToken =
                RefreshToken.create(
                        user,
                        tokenHash,
                        expiresAt
                );

        refreshTokenRepository.save(
                refreshToken
        );
    }

    @Transactional(readOnly = true)
    public User validateRefreshToken(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh Token이 없습니다.");
        }

        String tokenType = jwtProvider.getTokenType(rawRefreshToken);

        if (!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("올바른 Refresh Token이 아닙니다.");
        }

        Long userId = jwtProvider.getUserId(rawRefreshToken);

        int tokenVersion = jwtProvider.getTokenVersion(rawRefreshToken);

        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);

        RefreshToken savedToken =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException("유효하지 않은 Refresh Token입니다.")
                        );

        if (savedToken.isRevoked()) {
            throw new IllegalArgumentException("폐기된 Refresh Token입니다.");
        }

        if (savedToken.isExpired()) {
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }

        User user = savedToken.getUser();

        if (!user.getId().equals(userId)) {
            throw new IllegalArgumentException("Refresh Token 사용자 정보가 일치하지 않습니다.");
        }

        if (user.getTokenVersion() != tokenVersion ||
                savedToken.getTokenVersion() != tokenVersion) {
            throw new IllegalArgumentException("Refresh Token 버전이 일치하지 않습니다.");
        }

        if (userWithdrawalRequestRepository.existsByUser_Id(user.getId())) {
            throw new IllegalArgumentException("탈퇴 신청 상태에서는 토큰을 재발급할 수 없습니다.");
        }

        return user;
    }

    @Transactional
    public User startLoginSession(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        // 기존 Access Token 즉시 무효화
        user.increaseTokenVersion();

        // 기존 Refresh Token 제거
        refreshTokenRepository.deleteAllByUser_Id(userId);

        refreshTokenRepository.flush();

        return user;
    }

    @Transactional
    public User findOrCreateKakaoUser(
            Long kakaoId,
            String email,
            String nickname
    ) {

        String providerUserId =
                String.valueOf(kakaoId);

        SocialAccount socialAccount =
                socialAccountRepository
                        .findByProviderAndProviderUserId(
                                SocialProvider.KAKAO,
                                providerUserId
                        )
                        .orElse(null);

        if (socialAccount != null) {return socialAccount.getUser();}

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            String randomPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());

            user = User.createSocial(email, randomPassword, nickname);

            userRepository.save(user);
        }

        SocialAccount newSocialAccount = SocialAccount.create(user, SocialProvider.KAKAO, providerUserId);

        socialAccountRepository.save(newSocialAccount);

        return user;
    }

    @Transactional
    public void recoverSocialAccount(Long userId) {

        if (userWithdrawalRequestRepository.existsByUser_Id(userId)) {
            userWithdrawalRequestRepository.deleteByUser_Id(userId);
        }
    }
}