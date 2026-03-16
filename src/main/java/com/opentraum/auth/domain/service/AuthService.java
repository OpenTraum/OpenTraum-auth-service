package com.opentraum.auth.domain.service;

import com.opentraum.auth.domain.dto.AuthResponse;
import com.opentraum.auth.domain.dto.LoginRequest;
import com.opentraum.auth.domain.dto.SignupRequest;
import com.opentraum.auth.domain.entity.Role;
import com.opentraum.auth.domain.entity.User;
import com.opentraum.auth.domain.repository.UserRepository;
import com.opentraum.auth.global.exception.BusinessException;
import com.opentraum.auth.global.exception.ErrorCode;
import com.opentraum.auth.global.util.RedisKeyGenerator;
import com.opentraum.auth.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

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

                    User user = User.builder()
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .name(request.getName())
                            .phone(request.getPhone())
                            .role(Role.USER.name())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return userRepository.save(user)
                            .map(saved -> {
                                String token = jwtProvider.createToken(
                                        saved.getId(), saved.getEmail(), saved.getRole());
                                return AuthResponse.builder()
                                        .userId(saved.getId())
                                        .email(saved.getEmail())
                                        .name(saved.getName())
                                        .role(saved.getRole())
                                        .token(token)
                                        .build();
                            });
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

                    String token = jwtProvider.createToken(
                            user.getId(), user.getEmail(), user.getRole());
                    return Mono.just(AuthResponse.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .role(user.getRole())
                            .token(token)
                            .build());
                });
    }

    /**
     * 로그아웃
     * - 토큰 남은 만료시간만큼 Redis 블랙리스트에 등록
     */
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
