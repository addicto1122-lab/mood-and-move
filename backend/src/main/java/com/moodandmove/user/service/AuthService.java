package com.moodandmove.user.service;

import com.moodandmove.user.domain.entity.ConsentPolicy;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.domain.entity.UserConsent;
import com.moodandmove.user.domain.type.ConsentType;
import com.moodandmove.user.dto.request.LoginRequest;
import com.moodandmove.user.dto.request.SignupRequest;
import com.moodandmove.user.repository.ConsentPolicyRepository;
import com.moodandmove.user.repository.UserConsentRepository;
import com.moodandmove.user.repository.UserRepository;
import com.moodandmove.user.repository.UserWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserWithdrawalRequestRepository userWithdrawalRequestRepository;
    private final ConsentPolicyRepository consentPolicyRepository;
    private final UserConsentRepository userConsentRepository;

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
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

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
}