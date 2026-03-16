package com.opentraum.auth.global.util;

/**
 * Redis 키 생성을 위한 유틸리티 클래스
 */
public class RedisKeyGenerator {

    private RedisKeyGenerator() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * JWT 블랙리스트 키 (로그아웃 시 토큰 무효화)
     * blacklist:{token}
     */
    public static String blacklistKey(String token) {
        return "blacklist:" + token;
    }
}
