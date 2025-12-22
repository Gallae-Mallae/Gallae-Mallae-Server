package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.repository.AttractionRepository;
import com.practice.OAuth2.domain.plan.dto.ScheduleCreateRequest;
import com.practice.OAuth2.domain.plan.entity.Plan;
import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import com.practice.OAuth2.domain.plan.repository.PlanMemberRepository;
import com.practice.OAuth2.domain.plan.repository.PlanRepository;
import com.practice.OAuth2.domain.plan.repository.ScheduleBlockRepository;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.practice.OAuth2.domain.plan.dto.ScheduleBlockResponse;

@Service
@RequiredArgsConstructor
@Transactional
// 스케줄 블록을 옮겼을 때, publish 하기
public class ScheduleService {

    private final PlanRepository planRepository;
    private final AttractionRepository attractionRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final PlanMemberRepository planMemberRepository;

    // 알림 전송용(web socket)
    private final SimpMessagingTemplate messagingTemplate;

    // 동시성 제어용(redis lock)
    private final StringRedisTemplate redisTemplate;

    // Redis Lock 키 접두사
    private static final String LOCK_PREFIX = "plan:lock:";

    // 내 여행계획의 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleBlockResponse> getSchedules(Long userId, Long planId) {

        // 권한 체크
        boolean isMember = planMemberRepository.existsByPlan_PlanIdAndUser_UserIdAndLeftAtIsNull(planId, userId);

        if (!isMember) {
            throw new IllegalArgumentException("이 여행에 접근할 권한이 없습니다.");
        }

        // 스케줄 조회 (날짜 -> 시간 순)
        List<ScheduleBlock> blocks = scheduleBlockRepository.findAllByPlan_PlanIdOrderByDayAscStartTimeAsc(planId);

        // DTO 변환
        return blocks.stream()
                .map(ScheduleBlockResponse::new)
                .collect(Collectors.toList());
    }

    // 블록 생성
    public void createScheduleBlock(Long planId, ScheduleCreateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) {
            throw new IllegalStateException("현재 다른 사용자가 편집 중입니다. 잠시 후 시도해주세요.");
        }

        try{
            // 장소 정보 조회 (null일 수 있음)
            Attraction attraction = null;
            if (request.getAttractionId() != null) {
                attraction = attractionRepository.findById(request.getAttractionId())
                        .orElse(null);
            }

            // 엔티티 생성 (기본 30분 설정)
            ScheduleBlock block = ScheduleBlock.builder()
                    .plan(plan)
                    .attraction(attraction)
                    .day(request.getDay())
                    .startTime(request.getStartTime())
                    .endTime(request.getStartTime().plusMinutes(30)) // 기본 30분
                    .title(request.getTitle()) // 장소명이거나 사용자 입력 제목
                    .build();

            scheduleBlockRepository.save(block);

            // 엔티티 -> DTO 변환 후 전송
            ScheduleBlockResponse response = new ScheduleBlockResponse(block);

            // [STOMP] 실시간 알림 : 생성된 블록 정보를 방 전체에 전송 (Type: CREATE)
            sendStompMessage(planId, "BLOCK_CREATED", response);

        }finally{
            // lock 해제
            unlock(lockKey);
        }
    }

    // 블록 크기 조절
    public void resizeScheduleBlock(Long blockId) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        // [STOMP] 변경된 정보 전송 (Type: UPDATE)
        Long planId = block.getPlan().getPlanId();

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) {
            throw new IllegalStateException("잠시 후 다시 시도해주세요.");
        }

        try{
            LocalTime currentEndTime = block.getEndTime();
            LocalTime newEndTime = currentEndTime.plusMinutes(30);

            // 시간 업데이트 (엔티티 내부에 updateEndTime 메서드 필요)
            block.updateEndTime(newEndTime);

            ScheduleBlockResponse response = new ScheduleBlockResponse(block);

            sendStompMessage(planId, "BLOCK_RESIZED", response);
        }finally{
            unlock(lockKey);
        }
    }

    // 블록 이동
    public void moveScheduleBlock(Long userId, Long blockId, Integer newDay, LocalTime newStartTime) {
        // 1. 블록 조회
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        Long planId = block.getPlan().getPlanId();

        // [Redis Lock] 획득
        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) {
            throw new IllegalStateException("동시 편집 충돌! 잠시 후 다시 시도하세요.");
        }

        // 이 방 멤버인지 확인 필요?

        try{
            // 블록 옮겼을때 바뀐 정보 업데이트
            block.changePosition(newDay, newStartTime);

            ScheduleBlockResponse response = new ScheduleBlockResponse(block);
            // 변경된 블록 정보만 보내거나, 해당 날짜의 전체 리스트를 보내서 덮어씌우게 함
            // "UPDATE"라는 신호와 함께 변경된 블록 정보를 보냄
            sendStompMessage(planId, "BLOCK_MOVED", response);
        }finally{
            unlock(lockKey);
        }
    }

    // 블록 삭제
    public void deleteScheduleBlock(Long userId, Long blockId) {
        // 1. 블록 조회
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        // 권한 체크

        Long planId = block.getPlan().getPlanId();
        String lockKey = LOCK_PREFIX + planId;

        if (!tryLock(lockKey)) {
            throw new IllegalStateException("잠시 후 다시 시도해주세요.");
        }

        try {
            // 삭제 (Soft Delete: 엔티티의 @SQLDelete 작동)
            scheduleBlockRepository.delete(block);

            // [STOMP] 삭제 알림 전송
            // 삭제된 블록의 ID만 보내도 되지만, 프론트 처리를 위해 기존처럼 전체 정보를 보내줍니다.
            // (프론트에서 "어떤 블록이 삭제됐는지" 확인 후 DOM에서 제거)
            ScheduleBlockResponse response = new ScheduleBlockResponse(block);
            sendStompMessage(planId, "BLOCK_DELETED", response);

        } finally {
            unlock(lockKey);
        }
    }

    /**
     * STOMP 메시지 전송 공통 메서드
     * 구독 주소: /topic/plans/{planId}
     * 메시지 구조:
     * { "event": "이벤트명",
     * "data": { ... }
     * }
     */
    private void sendStompMessage(Long planId, String eventType, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("event", eventType);
        message.put("data", data);

        messagingTemplate.convertAndSend("/topic/plans/" + planId, message);
    }

    /**
     * Redis 분산 락 획득 (Simple Implementation)
     * - Key: plan:lock:{planId}
     * - TTL: 3초 (데드락 방지용)
     */
    private boolean tryLock(String key) {
        // setIfAbsent = Redis SETNX 명령어 (값이 없을 때만 set 성공)
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, "locked", 3, TimeUnit.SECONDS)
        );
    }

    // redis lock 제거
    private void unlock(String key) {
        redisTemplate.delete(key);
    }
}
