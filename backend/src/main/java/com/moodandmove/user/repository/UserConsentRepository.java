package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.UserConsent;
import com.moodandmove.user.domain.type.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    Optional<UserConsent>
    findFirstByUser_IdAndPolicy_ConsentTypeAndPolicy_ActiveTrueOrderByPolicy_EffectiveAtDesc(
            Long userId, ConsentType consentType );

    Optional<UserConsent> findByUser_IdAndPolicy_Id(
            Long userId,
            Long policyId
    );
}