package com.opentraum.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 토큰 생성
     */
    public String createToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 Claims 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 토큰에서 역할(Role) 추출
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 토큰의 남은 유효 시간 (밀리초)
     */
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = getClaims(token);
            long expiration = claims.getExpiration().getTime();
            long remaining = expiration - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (JwtException e) {
            return 0;
        }
    }
}
