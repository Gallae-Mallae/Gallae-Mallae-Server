package com.practice.OAuth2.domain.auth.service;

import com.practice.OAuth2.domain.auth.dto.TokenResponse;
import com.practice.OAuth2.domain.auth.entity.RefreshToken;
import com.practice.OAuth2.domain.auth.repository.RefreshTokenRepository;
import com.practice.OAuth2.global.exception.BadRequestException;
import com.practice.OAuth2.global.security.TokenProvider;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service @Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new BadRequestException("리프레시 토큰을 받지 못했습니다.");
        }

        // 10초 유예기간 캐시저장소의 키 이름 설정 : 어떤 Old Token(파라미터로 온 refreshToken)이 요청했는지를 식별하기 위함
        String graceKey = "grace_period:" + refreshToken;

        /* 레디스 캐시저장소에 Old Token(파라미터로 온 refreshToken) 이름이 포함된 키를 가진 캐시 엔트리가 존재한다면
           1등이 Old Token 을 써서 재발급 했었다는 뜻 -> 이 캐시 엔트리는 재발급된 리프레시 토큰이므로 바로 이 데이터 응답,
           캐시엔트리 유예기간 : 10초 설정 */
        TokenResponse cachedToken = (TokenResponse) redisTemplate.opsForValue().get(graceKey);
        if (cachedToken != null) {
            log.info("Grace Period 적중");
            return cachedToken;
        }

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 브라우저에게 쿠키로 받은 리프레시 토큰과 Redis 저장소(TTL: 14일)에 있는 리프레시토큰을 비교
        String userId = String.valueOf(tokenProvider.getUserIdFromToken(refreshToken));
        // 속도를 위해 PK (id) 사용
        RefreshToken redisToken = refreshTokenRepository.findById(userId).orElse(null);

        if (redisToken == null) {
            throw new BadRequestException("만료된 리프레시 토큰입니다.");
        } else if (!redisToken.getToken().equals(refreshToken)) {
            throw new BadRequestException("리프레시 토큰이 만료되었습니다.");
        }

        // 재발급 로직

        // 새 토큰 생성을 위한 Authentication 객체 생성 (ID만 있으면 됨)
        // TokenProvider는 토큰을 만들 때 Authentication 안에 있는 UserPrincipal을 꺼내고, 그 안의 id를 써서 토큰을 만들도록 되어있음
        UserDetails principal = UserPrincipal.create(Long.parseLong(userId));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

        // 새 토큰 발급 (RTR: Refresh Token도 새로 발급)
        String newAccessToken = tokenProvider.createToken(authentication);
        String newRefreshToken = tokenProvider.createRefreshToken(authentication);

        // Redis 업데이트
        refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken));

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();

        // 1등이 Old Token(파라미터로 온 refreshToken) 이름이 포함된 키에 재발급한 리프레시 토큰을 value 로 설정
        redisTemplate.opsForValue().set(graceKey, tokenResponse, 10, TimeUnit.SECONDS);

        return tokenResponse;
    }

    @Transactional
    public void logout(String refreshToken) {

        if (refreshToken == null) {
            throw new BadRequestException("리프레시 토큰이 없습니다.");
        }

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("유효하지 않은 리프레시 토큰입니다.");
        }

        String userId = String.valueOf(tokenProvider.getUserIdFromToken(refreshToken));
        refreshTokenRepository.deleteById(userId);

    }
}