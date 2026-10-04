package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.ConsentPolicy;
import com.moodandmove.user.domain.type.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConsentPolicyRepository
        extends JpaRepository<ConsentPolicy, Long> {

    Optional<ConsentPolicy>
    findFirstByConsentTypeAndActiveTrueAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
            ConsentType consentType,
            LocalDateTime now
    );
}