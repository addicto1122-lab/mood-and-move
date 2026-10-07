package com.moodandmove.common.security;

import com.moodandmove.user.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }


    public String createAccessToken(User user) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + accessTokenExpiration
        );

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(
                        "tokenVersion",
                        user.getTokenVersion()
                )
                .claim(
                        "tokenType",
                        "ACCESS"
                )
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    public String createRefreshToken(User user) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + refreshTokenExpiration
        );

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(
                        "tokenVersion",
                        user.getTokenVersion()
                )
                .claim(
                        "tokenType",
                        "REFRESH"
                )
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }


    public Claims parseToken(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {

        Claims claims =
                parseToken(token);

        return Long.valueOf(
                claims.getSubject()
        );
    }

    public int getTokenVersion(String token) {

        Claims claims =
                parseToken(token);

        return claims.get(
                "tokenVersion",
                Integer.class
        );
    }

    public String getTokenType(String token) {

        Claims claims =
                parseToken(token);

        return claims.get(
                "tokenType",
                String.class
        );
    }

    public Date getExpiration(String token) {

        return parseToken(token)
                .getExpiration();
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpiration;
    }
}