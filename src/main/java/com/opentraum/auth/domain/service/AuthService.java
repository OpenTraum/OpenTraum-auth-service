package com.opentraum.auth.domain.service;

import com.opentraum.auth.domain.dto.AuthResponse;
import com.opentraum.auth.domain.dto.LoginRequest;
import com.opentraum.auth.domain.dto.SignupRequest;
import com.opentraum.auth.domain.entity.RefreshToken;
import com.opentraum.auth.domain.entity.Role;
import com.opentraum.auth.domain.entity.User;
import com.opentraum.auth.domain.repository.RefreshTokenRepository;
import com.opentraum.auth.domain.repository.UserRepository;
import com.opentraum.auth.global.exception.BusinessException;
import com.opentraum.auth.global.exception.ErrorCode;
import com.opentraum.auth.global.util.RedisKeyGenerator;
import com.opentraum.auth.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    /**
     * 회원가입
     * - 이메일 중복 체크
     * - 비밀번호 인코딩 후 사용자 저장
     * - JWT 토큰 생성 및 반환
     */
    public Mono<AuthResponse> signup(SignupRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.<AuthResponse>error(new BusinessException(ErrorCode.DUPLICATE_EMAIL));
                    }

                    Role role;
                    try {
                        role = Role.valueOf(request.getRole());
                    } catch (IllegalArgumentException e) {
                        return Mono.<AuthResponse>error(new BusinessException(ErrorCode.INVALID_ROLE));
                    }

                    String tenantId = null;
                    if (role == Role.ORGANIZER) {
                        tenantId = generateSlug(request.getName());
                    }

                    User user = User.builder()
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .name(request.getName())
                            .phone(request.getPhone())
                            .role(role.name())
                            .tenantId(tenantId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return userRepository.save(user)
                            .flatMap(saved -> issueRefreshToken(saved)
                                    .map(refresh -> {
                                        String token = jwtProvider.createToken(
                                                saved.getId(), saved.getEmail(), saved.getRole(), saved.getTenantId());
                                        return AuthResponse.builder()
                                                .userId(saved.getId())
                                                .email(saved.getEmail())
                                                .name(saved.getName())
                                                .role(saved.getRole())
                                                .tenantId(saved.getTenantId())
                                                .token(token)
                                                .refreshToken(refresh.getRefreshToken())
                                                .build();
                                    }));
                });
    }

    /**
     * 로그인
     * - 이메일로 사용자 조회
     * - 비밀번호 검증
     * - JWT 토큰 생성 및 반환
     */
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED)))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.<AuthResponse>error(new BusinessException(ErrorCode.UNAUTHORIZED));
                    }

                    return issueRefreshToken(user)
                            .map(refresh -> {
                                String token = jwtProvider.createToken(
                                        user.getId(), user.getEmail(), user.getRole(), user.getTenantId());
                                return AuthResponse.builder()
                                        .userId(user.getId())
                                        .email(user.getEmail())
                                        .name(user.getName())
                                        .role(user.getRole())
                                        .tenantId(user.getTenantId())
                                        .token(token)
                                        .refreshToken(refresh.getRefreshToken())
                                        .build();
                            });
                });
    }

    /**
     * Refresh Token으로 새 access token 발급.
     * - DB에서 refresh token 조회 → 만료 체크 → 사용자 조회 → 새 access 발급
     * - Refresh rotation 안 함 (기존 refresh 그대로 유지)
     */
    public Mono<AuthResponse> refresh(String refreshTokenValue) {
        return refreshTokenRepository.findByRefreshToken(refreshTokenValue)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED)))
                .flatMap(refresh -> {
                    if (refresh.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return Mono.<AuthResponse>error(new BusinessException(ErrorCode.UNAUTHORIZED));
                    }
                    return userRepository.findById(refresh.getUserId())
                            .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.UNAUTHORIZED)))
                            .map(user -> {
                                String newAccess = jwtProvider.createToken(
                                        user.getId(), user.getEmail(), user.getRole(), user.getTenantId());
                                return AuthResponse.builder()
                                        .userId(user.getId())
                                        .email(user.getEmail())
                                        .name(user.getName())
                                        .role(user.getRole())
                                        .tenantId(user.getTenantId())
                                        .token(newAccess)
                                        .refreshToken(refreshTokenValue)
                                        .build();
                            });
                });
    }

    private Mono<RefreshToken> issueRefreshToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .refreshToken(UUID.randomUUID().toString())
                .tenantId(user.getTenantId() != null ? user.getTenantId() : "default")
                .createdAt(now)
                .expiresAt(now.plusNanos(refreshExpirationMs * 1_000_000L))
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * 로그아웃
     * - 토큰 남은 만료시간만큼 Redis 블랙리스트에 등록
     */
    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9가-힣\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isEmpty()) {
            slug = "org";
        }
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return slug + "-" + suffix;
    }

    public Mono<Void> logout(String token) {
        long remainingMs = jwtProvider.getRemainingExpiration(token);
        if (remainingMs <= 0) {
            return Mono.empty();
        }
        String blacklistKey = RedisKeyGenerator.blacklistKey(token);
        return redisTemplate.opsForValue()
                .set(blacklistKey, "revoked", Duration.ofMillis(remainingMs))
                .then();
    }
}
