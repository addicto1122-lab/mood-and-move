package com.moodandmove.user.domain.entity;

import com.moodandmove.user.domain.type.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_social_accounts_provider_user",
                        columnNames = {
                                "provider",
                                "provider_user_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_social_accounts_user_provider",
                        columnNames = {
                                "user_id",
                                "provider"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "provider",
            nullable = false,
            length = 20
    )
    private SocialProvider provider;

    @Column(
            name = "provider_user_id",
            nullable = false,
            length = 100
    )
    private String providerUserId;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    private SocialAccount(
            User user,
            SocialProvider provider,
            String providerUserId
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static SocialAccount create(
            User user,
            SocialProvider provider,
            String providerUserId
    ) {
        return new SocialAccount(
                user,
                provider,
                providerUserId
        );
    }
}