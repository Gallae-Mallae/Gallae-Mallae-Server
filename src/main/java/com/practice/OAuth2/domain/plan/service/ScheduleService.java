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
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
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

    private final TransactionTemplate transactionTemplate;

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
            // 개별 트랜잭션 시작
            ScheduleBlockResponse response = transactionTemplate.execute(status -> {
                Attraction attraction = null;
                if (request.getAttractionId() != null) {
                    attraction = attractionRepository.findById(request.getAttractionId()).orElse(null);
                }
                ScheduleBlock block = ScheduleBlock.builder()
                        .plan(plan).attraction(attraction)
                        .day(request.getDay())
                        .startTime(request.getStartTime())
                        .endTime(request.getStartTime().plusMinutes(30))
                        .title(request.getTitle()).build();

                scheduleBlockRepository.save(block);

                return new ScheduleBlockResponse(block);
            });
            // [STOMP] 실시간 알림 : 생성된 블록 정보를 방 전체에 전송
            sendStompMessage(planId, "BLOCK_CREATED", response);

        }finally{
            // lock 해제
            unlock(lockKey);
        }
    }

    // 블럭 크기 조절
    public void resizeScheduleBlock(Long blockId, LocalTime newEndTime) {
        ScheduleBlock tempblock = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("블록 x"));
        Long planId = tempblock.getPlan().getPlanId();

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도");

        try {
            ScheduleBlockResponse response = transactionTemplate.execute(status -> {
                ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                        .orElseThrow(() -> new IllegalArgumentException("블록 x"));

                block.updateEndTime(newEndTime);

                return new ScheduleBlockResponse(block);
            });
            sendStompMessage(planId, "BLOCK_RESIZED", response);
        }finally{
            unlock(lockKey);
        }
    }

    // 블럭 이동
    public void moveScheduleBlock(Long userId, Long blockId, Integer newDay, LocalTime newStartTime) {
        ScheduleBlock tempblock = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("블록 없음"));
        Long planId = tempblock.getPlan().getPlanId();

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도");

        try {
            ScheduleBlockResponse response = transactionTemplate.execute(status -> {
                ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                        .orElseThrow(() -> new IllegalArgumentException("블록 없음"));

                Integer oldDay = block.getDay();
                block.changePosition(newDay, newStartTime);

                ScheduleBlockResponse dto = new ScheduleBlockResponse(block);
                dto.setFromDay(oldDay);
                return dto;
            });

            // 변경된 블럭 정보만 보내거나, 해당 날짜의 전체 리스트를 보내서 덮어씌우게 함
            sendStompMessage(planId, "BLOCK_MOVED", response);
        }finally{
            unlock(lockKey);
        }
    }

    // 블럭 삭제
    public void deleteScheduleBlock(Long userId, Long blockId) {
        ScheduleBlock tempblock = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("블록 x"));
        Long planId = tempblock.getPlan().getPlanId();

        String lockKey = LOCK_PREFIX + planId;
        if (!tryLock(lockKey)) throw new IllegalStateException("잠시 후 시도");

        try {
            // 트랜잭션 시작
            ScheduleBlockResponse response = transactionTemplate.execute(status -> {
                ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                        .orElseThrow(() -> new IllegalArgumentException("블록 x"));

                ScheduleBlockResponse dto = new ScheduleBlockResponse(block);

                scheduleBlockRepository.delete(block);

                return dto;
            });
            sendStompMessage(planId, "BLOCK_DELETED", response);

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
