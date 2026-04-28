package com.opentraum.auth.domain.repository;

import com.opentraum.auth.domain.entity.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {

    Mono<RefreshToken> findByRefreshToken(String refreshToken);

    Mono<Void> deleteByUserId(Long userId);
}
