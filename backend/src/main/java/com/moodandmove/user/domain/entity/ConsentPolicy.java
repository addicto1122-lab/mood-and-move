package com.moodandmove.user.domain.entity;

import com.moodandmove.user.domain.type.ConsentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "consent_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "consent_type",
            nullable = false,
            length = 50
    )
    private ConsentType consentType;

    @Column(
            name = "version",
            nullable = false,
            length = 20
    )
    private String version;

    @Column(
            name = "title",
            nullable = false,
            length = 100
    )
    private String title;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(
            name = "required",
            nullable = false
    )
    private boolean required;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    @Column(
            name = "effective_at",
            nullable = false
    )
    private LocalDateTime effectiveAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}