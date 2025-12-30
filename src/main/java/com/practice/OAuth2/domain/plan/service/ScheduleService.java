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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.practice.OAuth2.domain.plan.dto.ScheduleBlockResponse;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Transactional
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

    //private final TransactionTemplate transactionTemplate;

    // 여행계획 스케줄 전체 블럭 조회
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

    // day별 스케줄 블럭 조회
    @Transactional(readOnly = true)
    public List<ScheduleBlockResponse> getSchedulesByDay(Long userId, Long planId, Integer day) {
        // 권한 체크
        boolean isMember = planMemberRepository.existsByPlan_PlanIdAndUser_UserIdAndLeftAtIsNull(planId, userId);
        if (!isMember) {
            throw new IllegalArgumentException("이 여행에 접근할 권한이 없습니다.");
        }

        // day의 스케줄만 조회
        List<ScheduleBlock> blocks = scheduleBlockRepository.findAllByPlan_PlanIdAndDayOrderByStartTimeAsc(planId, day);

        // DTO 변환
        return blocks.stream()
                .map(ScheduleBlockResponse::new)
                .collect(Collectors.toList());
    }

    // 블럭 생성
    public void createScheduleBlock(Long planId, ScheduleCreateRequest request) {
        Plan plan = planRepository.findById(planId).orElseThrow(() -> new IllegalArgumentException("여행 x"));

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도");

        try {
            Attraction attraction = null;
            if (request.getAttractionId() != null) {
                attraction = attractionRepository.findById(request.getAttractionId()).orElse(null);
            }

            ScheduleBlock block = ScheduleBlock.builder()
                    .plan(plan)
                    .attraction(attraction)
                    .day(request.getDay())
                    .startTime(request.getStartTime())
                    .endTime(request.getStartTime().plusMinutes(30))
                    .title(request.getTitle())
                    .build();

            // DB 반영
            scheduleBlockRepository.saveAndFlush(block);

            ScheduleBlockResponse response = new ScheduleBlockResponse(block);

            // 커밋 후 알림 전송
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendStompMessage(planId, "BLOCK_CREATED", response);
                }
            });
        }finally{
            // lock 해제
            unlock(lockKey);
        }
    }

    // 블럭 크기 조절
    public void resizeScheduleBlock(Long blockId, LocalTime newEndTime) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));
        Long planId = block.getPlan().getPlanId();

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 다시 시도해주세요.");

        try {
            block.updateEndTime(newEndTime);
            scheduleBlockRepository.saveAndFlush(block);

            ScheduleBlockResponse response = new ScheduleBlockResponse(block);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendStompMessage(planId, "BLOCK_RESIZED", response);
                }
            });
        }finally{
            unlock(lockKey);
        }
    }

    // 블럭 이동
    public void moveScheduleBlock(Long userId, Long blockId, Integer newDay, LocalTime newStartTime) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        Integer oldDay = block.getDay();
        Long planId = block.getPlan().getPlanId();
        String lockKey = LOCK_PREFIX + planId;

        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 다시 시도해주세요.");

        try {
            // 1. 데이터 변경
            block.changePosition(newDay, newStartTime);

            // 2. DB 반영 (Flush)
            scheduleBlockRepository.saveAndFlush(block);

            // 3. 소켓 데이터 준비
            Map<String, Object> socketData = new HashMap<>();
            socketData.put("blockId", blockId);
            socketData.put("fromDay", oldDay);
            socketData.put("toDay", newDay);
            socketData.put("blockDetail", new ScheduleBlockResponse(block));

            // 4. 커밋 후 데이터 전송
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendStompMessage(planId, "BLOCK_MOVED", socketData);
                }
            });
        }finally{
            unlock(lockKey);
        }
    }

    // 블럭 삭제
    public void deleteScheduleBlock(Long userId, Long blockId) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));
        Long planId = block.getPlan().getPlanId();

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 다시 시도해주세요.");

        try {
            scheduleBlockRepository.delete(block);
            scheduleBlockRepository.flush(); // 즉시 쿼리 수행

            ScheduleBlockResponse response = new ScheduleBlockResponse(block);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendStompMessage(planId, "BLOCK_DELETED", response);
                }
            });

        } finally {
            unlock(lockKey);
        }
    }

    // STOMP 메시지 전송 공통 메서드
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
