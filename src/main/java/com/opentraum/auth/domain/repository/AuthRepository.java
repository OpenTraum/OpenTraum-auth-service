package com.opentraum.auth.domain.repository;

import com.opentraum.auth.domain.entity.Auth;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AuthRepository extends R2dbcRepository<Auth, Long> {

    Mono<Auth> findByRefreshToken(String refreshToken);

    Mono<Auth> findByUserIdAndTenantId(Long userId, String tenantId);

    Mono<Void> deleteByRefreshToken(String refreshToken);

    Mono<Void> deleteByUserId(Long userId);
}
