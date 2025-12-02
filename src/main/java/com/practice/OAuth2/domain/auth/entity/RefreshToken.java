package com.practice.OAuth2.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// value: Redis 키의 prefix (예: refreshToken:{userId})
// timeToLive: 초 단위 유효기간 (14일 = 1209600초) -> 이 시간이 지나면 Redis에서 자동 삭제됨
// Redis 캐시 저장소에 저장되는 데이터이므로 @Entity 사용 X (DB 데이터가 아님)
@RedisHash(value = "refreshToken", timeToLive = 1209600)
@AllArgsConstructor
@Getter
public class RefreshToken {

    @Id
    private String userId; // Key (유저 ID)

    private String token;  // Value (리프레시 토큰 값)
}