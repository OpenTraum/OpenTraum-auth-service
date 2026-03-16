package com.opentraum.auth.domain.service;

import com.opentraum.auth.domain.dto.LoginRequest;
import com.opentraum.auth.domain.dto.LoginResponse;
import com.opentraum.auth.domain.dto.TokenRefreshRequest;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * 로그인 처리
     * - 사용자 인증 후 access/refresh 토큰 발급
     */
    Mono<LoginResponse> login(LoginRequest request);

    /**
     * 토큰 갱신
     * - refresh 토큰 검증 후 새로운 access/refresh 토큰 발급
     */
    Mono<LoginResponse> refresh(TokenRefreshRequest request);

    /**
     * 로그아웃 처리
     * - refresh 토큰 삭제 및 access 토큰 블랙리스트 등록
     */
    Mono<Void> logout(String accessToken, String refreshToken);
}
