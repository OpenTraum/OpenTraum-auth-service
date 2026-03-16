package com.opentraum.auth.domain.service;

import com.opentraum.auth.domain.dto.LoginRequest;
import com.opentraum.auth.domain.dto.LoginResponse;
import com.opentraum.auth.domain.dto.TokenRefreshRequest;
import com.opentraum.auth.domain.repository.AuthRepository;
import com.opentraum.auth.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        // TODO: User Service 연동 후 구현
        // 1. User Service에 사용자 인증 요청 (WebClient)
        // 2. 인증 성공 시 access/refresh 토큰 생성
        // 3. refresh 토큰을 DB 및 Redis에 저장
        // 4. LoginResponse 반환
        return Mono.empty();
    }

    @Override
    public Mono<LoginResponse> refresh(TokenRefreshRequest request) {
        // TODO: 구현 예정
        // 1. refresh 토큰 유효성 검증
        // 2. DB에서 refresh 토큰 조회
        // 3. 새로운 access/refresh 토큰 생성
        // 4. 기존 refresh 토큰 폐기 및 신규 저장 (Rotation)
        // 5. LoginResponse 반환
        return Mono.empty();
    }

    @Override
    public Mono<Void> logout(String accessToken, String refreshToken) {
        // TODO: 구현 예정
        // 1. access 토큰을 Redis 블랙리스트에 등록 (남은 TTL만큼)
        // 2. refresh 토큰을 DB 및 Redis에서 삭제
        return Mono.empty();
    }
}
