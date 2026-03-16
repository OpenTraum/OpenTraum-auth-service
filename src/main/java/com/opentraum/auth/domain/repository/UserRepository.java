package com.opentraum.auth.domain.repository;

import com.opentraum.auth.domain.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
}
