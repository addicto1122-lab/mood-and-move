package com.moodandmove.user.dto.response;

import java.time.LocalDateTime;

public record WithdrawalStatusResponse(
        boolean pending,
        LocalDateTime requestedAt,
        LocalDateTime deletionScheduledAt
) {
}