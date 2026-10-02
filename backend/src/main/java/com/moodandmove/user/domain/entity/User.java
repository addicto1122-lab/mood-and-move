package com.moodandmove.user.domain.entity;

import com.moodandmove.user.domain.type.AgeGroup;
import com.moodandmove.user.domain.type.Gender;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", length = 20)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private User(
            String email,
            String passwordHash,
            String nickname
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }

    public static User create(
            String email,
            String passwordHash,
            String nickname
    ) {
        return new User(email, passwordHash, nickname);
    }

    public void increaseTokenVersion() {
        this.tokenVersion++;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void completeOnboarding(
            AgeGroup ageGroup,
            Gender gender
    ) {
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.onboardingCompleted = true;
    }

    public void updateProfile(
            String nickname,
            AgeGroup ageGroup,
            Gender gender
    ) {
        this.nickname = nickname;
        this.ageGroup = ageGroup;
        this.gender = gender;
    }

}