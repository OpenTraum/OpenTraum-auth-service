package com.opentraum.auth.domain.controller;

import com.opentraum.auth.domain.dto.LoginRequest;
import com.opentraum.auth.domain.dto.LoginResponse;
import com.opentraum.auth.domain.dto.TokenRefreshRequest;
import com.opentraum.auth.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(response -> ResponseEntity.ok(response));
    }

    /**
     * 토큰 갱신
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<LoginResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request)
                .map(response -> ResponseEntity.ok(response));
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TokenRefreshRequest request) {

        String accessToken = authorization.replace("Bearer ", "");
        return authService.logout(accessToken, request.refreshToken())
                .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build()));
    }
}
