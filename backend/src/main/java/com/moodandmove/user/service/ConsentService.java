package com.moodandmove.user.service;

import com.moodandmove.user.domain.entity.ConsentPolicy;
import com.moodandmove.user.domain.entity.User;
import com.moodandmove.user.domain.entity.UserConsent;
import com.moodandmove.user.domain.type.ConsentType;
import com.moodandmove.user.dto.response.ConsentPolicyResponse;
import com.moodandmove.user.dto.response.LocationConsentResponse;
import com.moodandmove.user.repository.ConsentPolicyRepository;
import com.moodandmove.user.repository.UserConsentRepository;
import com.moodandmove.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentPolicyRepository consentPolicyRepository;
    private final UserConsentRepository userConsentRepository;
    private final UserRepository userRepository;

    private ConsentPolicy getCurrentPolicy() {
        return consentPolicyRepository
                .findFirstByConsentTypeAndActiveTrueAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                        ConsentType.CURRENT_LOCATION,
                        LocalDateTime.now()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "현재 위치 이용 약관을 찾을 수 없습니다."
                        )
                );
    }

    @Transactional(readOnly = true)
    public ConsentPolicyResponse getCurrentLocationPolicy() {

        ConsentPolicy policy = getCurrentPolicy();

        return new ConsentPolicyResponse(
                policy.getId(),
                policy.getConsentType().name(),
                policy.getVersion(),
                policy.getTitle(),
                policy.getContent(),
                policy.isRequired()
        );
    }

    @Transactional(readOnly = true)
    public LocationConsentResponse getLocationConsent(Long userId) {

        ConsentPolicy policy = getCurrentPolicy();

        boolean agreed =
                userConsentRepository
                        .findByUser_IdAndPolicy_Id(
                                userId,
                                policy.getId()
                        )
                        .map(UserConsent::isAgreed)
                        .orElse(false);

        return new LocationConsentResponse(
                policy.getId(),
                policy.getVersion(),
                policy.getTitle(),
                policy.getContent(),
                policy.isRequired(),
                agreed
        );
    }

    @Transactional
    public void updateLocationConsent(
            Long userId,
            boolean agreed
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        ConsentPolicy policy = getCurrentPolicy();

        UserConsent consent =
                userConsentRepository
                        .findByUser_IdAndPolicy_Id(
                                userId,
                                policy.getId()
                        )
                        .orElse(null);

        if (consent == null) {
            userConsentRepository.save(
                    UserConsent.create(
                            user,
                            policy,
                            agreed
                    )
            );

            return;
        }

        consent.changeConsent(agreed);

        userConsentRepository.save(consent);
    }
}