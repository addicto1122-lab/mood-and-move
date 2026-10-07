package com.moodandmove.user.repository;

import com.moodandmove.user.domain.entity.SocialAccount;
import com.moodandmove.user.domain.type.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository
        extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount>
    findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );
}