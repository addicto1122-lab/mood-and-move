package com.moodandmove.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_consents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_consents_user_policy",
                        columnNames = {"user_id", "policy_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "policy_id",
            nullable = false
    )
    private ConsentPolicy policy;

    @Column(
            name = "agreed",
            nullable = false
    )
    private boolean agreed;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    private UserConsent(
            User user,
            ConsentPolicy policy,
            boolean agreed
    ) {
        this.user = user;
        this.policy = policy;
        this.agreed = agreed;

        if (agreed) {
            this.agreedAt = LocalDateTime.now();
        }
    }

    public static UserConsent create(
            User user,
            ConsentPolicy policy,
            boolean agreed
    ) {
        return new UserConsent(
                user,
                policy,
                agreed
        );
    }

    public void changeConsent(boolean agreed) {
        this.agreed = agreed;

        if (agreed) {
            this.agreedAt = LocalDateTime.now();
            this.revokedAt = null;
        } else {
            this.revokedAt = LocalDateTime.now();
        }
    }
}