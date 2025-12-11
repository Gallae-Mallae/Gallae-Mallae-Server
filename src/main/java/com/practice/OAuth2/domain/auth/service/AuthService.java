package com.practice.OAuth2.domain.auth.service;

import com.practice.OAuth2.domain.auth.dto.TokenResponse;
import com.practice.OAuth2.domain.auth.entity.RefreshToken;
import com.practice.OAuth2.domain.auth.repository.RefreshTokenRepository;
import com.practice.OAuth2.global.exception.BadRequestException;
import com.practice.OAuth2.global.security.TokenProvider;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service @Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final RedissonClient redissonClient;      // 락 제어용
    private final RedisTemplate<String, Object> redisTemplate; // 결과 공유용 캐시 저장소

    // 1등 쓰레드의 결과물이 유지되는 시간 (3초)
    private static final long RESULT_TTL_MS = 3000;

//  Redis 는 @Transactional 제거 : Race Condition 대비해야하는데 .save()가 즉시 반영이 안되므로
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new BadRequestException("리프레시 토큰을 받지 못했습니다.");
        }

        // Key, value 지정 및 최적화 : 토큰이 너무 기니까 해싱해서 짧게 만듦 (메모리 절약 + 성능 향상)
        String hashedToken = sha256(refreshToken);
        String lockKey = "reissue:lock:" + hashedToken;
        String resultKey = "reissue:result:" + hashedToken; // 1등이 완료했을때 키 명

        // 완전 후발주자는 락 잡기 전에 캐시 확인해서 1등이 만든 캐시 엔트리가 있다면 바로 리턴
        TokenResponse cachedResult = (TokenResponse) redisTemplate.opsForValue().get(resultKey);
        if (cachedResult != null) {
            log.info("redisson lock 진입 전 Result Cashing 수행");
            return cachedResult;
        }

        // Redisson Lock 줄 서기
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // waitTime 3초: 2등은 3초간 얌전히 대기 (Pub/Sub 방식이라 서버 부하 적음)
            // leaseTime 5초: 1등이 죽어도 5초 뒤 자동 해제 (데드락 방지)
            boolean isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                // 락 획득 실패 (시스템 과부하 등) -> 마지막으로 정답지 한번 더 확인
                TokenResponse finalCheck = (TokenResponse) redisTemplate.opsForValue().get(resultKey);
                if (finalCheck != null) return finalCheck;

                throw new BadRequestException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 1등 락 풀고 진입 / 2등은 대기 후 진입

            // Double Check : 1등 이후의 쓰레드들은 1등이 끝낸 작업 (키 명: resultKey) 을 바로 리턴
            TokenResponse winnerResult = (TokenResponse) redisTemplate.opsForValue().get(resultKey);
            if (winnerResult != null) {
                log.info("Result Caching 수행");
                return winnerResult; // 2등부터 여기서 리턴 (DB 접근 안하게)
            }

            // =======================================================
            // 지금부터 1등 쓰레드만 실행하는 구역 (DB 작업)
            // =======================================================

            // 유효성 검사
            if (!tokenProvider.validateToken(refreshToken)) {
                throw new BadRequestException("유효하지 않은 토큰");
            }

            // 브라우저에게 쿠키로 받은 리프레시 토큰과 Redis 저장소(TTL: 14일)에 있는 리프레시토큰을 비교 하기 위한
            // 브라우저에게 받은 리프레시토큰의 레디스 키 명 추출
            String userId = String.valueOf(tokenProvider.getUserIdFromToken(refreshToken));

            // DB 상태 확인 (토큰 삭제/만료 여부 검증 : 속도를 위해 PK (id) 사용)
            RefreshToken dbToken = refreshTokenRepository.findById(userId).orElse(null);
            // 브라우저에게 받은 리프레시토큰이 Redis 저장소에 없거나 value 가 다르다면 예외
            if (dbToken == null || !dbToken.getToken().equals(refreshToken)) {
                throw new BadRequestException("이미 만료되었거나 갱신된 토큰입니다.");
            }

            // 재발급 로직
            // 새 토큰 생성을 위한 Authentication 객체 생성 (ID만 있으면 됨)
            // TokenProvider 는 토큰을 만들 때 Authentication 안에 있는 UserPrincipal을 꺼내고, 그 안의 id를 써서 토큰을 만들도록 되어있음
            UserDetails principal = UserPrincipal.create(Long.parseLong(userId));
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

            // 새 토큰 발급 (RTR: Refresh Token 도 새로 발급) 및 쿠키에 보낼 응답 설정
            String newAccessToken = tokenProvider.createToken(authentication);
            String newRefreshToken = tokenProvider.createRefreshToken(authentication);
            TokenResponse newTokenResponse = new TokenResponse(newAccessToken, newRefreshToken);

            // Redis DB 리프레시 토큰 업데이트 ( 기존 키에 값을 덮어 씌우기 )
            refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken));

            // 결과 공유 : 재발급 갱신 된 쿠키에 보낼 응답 등록 (TTL 3초), 키 resultKey : 값 newTokenResponse
            // 1등이 이걸 저장해야 2등부터 Double Check 에서 받아감
            redisTemplate.opsForValue().set(resultKey, newTokenResponse, RESULT_TTL_MS, TimeUnit.MILLISECONDS);

            log.info("첫번째 쓰레드로 DB 갱신 및 결과 공유 캐싱 완료");
            return newTokenResponse;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock process interrupted");
        } finally {
            // Safe Unlock : 안전한 락 해제
            try {
                // "락이 잠겨있고 && 내가 주인이면" 해제
                if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (IllegalMonitorStateException e) {
                // 이미 타임아웃으로 락이 사라졌거나, 다른 스레드가 푼 경우 (무시)
                log.warn("Redisson Lock already unlocked: {} - {}", lockKey, e.getMessage());
            }
        }
    }

    // SHA-256 해싱 유틸
    private String sha256(String original) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(original.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

//    @Transactional 제거 : Redis 는 즉시 반영
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