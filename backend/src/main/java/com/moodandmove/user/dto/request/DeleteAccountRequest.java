package com.moodandmove.user.dto.request;

public record DeleteAccountRequest(
        String currentPassword
) {
}