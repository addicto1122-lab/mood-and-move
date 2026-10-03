package com.moodandmove.user.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_withdrawal_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWithdrawalRequest {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(
            name = "requested_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestedAt;

    @Column(
            name = "deletion_scheduled_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime deletionScheduledAt;

    private UserWithdrawalRequest(User user) {
        LocalDateTime now = LocalDateTime.now();

        this.user = user;
        this.requestedAt = now;
        this.deletionScheduledAt = now.plusDays(7);
    }

    public static UserWithdrawalRequest create(User user) {
        return new UserWithdrawalRequest(user);
    }
}