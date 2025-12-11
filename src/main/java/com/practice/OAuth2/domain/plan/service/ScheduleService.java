package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.repository.AttractionRepository;
import com.practice.OAuth2.domain.plan.dto.ScheduleCreateRequest;
import com.practice.OAuth2.domain.plan.entity.Plan;
import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import com.practice.OAuth2.domain.plan.repository.PlanRepository;
import com.practice.OAuth2.domain.plan.repository.ScheduleBlockRepository;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
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
    private final SimpMessagingTemplate messagingTemplate;

    // 블록 생성
    public void createScheduleBlock(Long planId, ScheduleCreateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

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
        // [STOMP] 생성된 블록 정보를 방 전체에 전송 (Type: CREATE)
        messagingTemplate.convertAndSend("/topic/plans/" + planId + "/schedules/create", response);
    }

    // 블록 크기 조절
    public void resizeScheduleBlock(Long blockId, LocalTime newEndTime) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        // 시간 업데이트 (엔티티 내부에 updateEndTime 메서드 필요)
        block.updateEndTime(newEndTime);

        // [STOMP] 변경된 정보 전송 (Type: UPDATE)
        Long planId = block.getPlan().getPlanId();

        ScheduleBlockResponse response = new ScheduleBlockResponse(block);
        messagingTemplate.convertAndSend("/topic/plans/" + planId + "/schedules/update", response);
    }

    // 블록 이동
    public void moveScheduleBlock(Long userId, Long blockId, Integer newDay, LocalTime newStartTime) {
        // 1. 블록 조회
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 블록입니다."));

        // 이 방 멤버인지 확인 필요?

        // 2. 블록 옮겼을때 바뀐 정보 업데이트
        block.changePosition(newDay, newStartTime);

        // 3. [STOMP] 실시간 동기화
        // "10번 방의 스케줄이 변경되었으니, 새로고침"
        Long planId = block.getPlan().getPlanId();

        ScheduleBlockResponse response = new ScheduleBlockResponse(block);
        // 변경된 블록 정보만 보내거나, 해당 날짜의 전체 리스트를 보내서 덮어씌우게 함
        // "UPDATE"라는 신호와 함께 변경된 블록 정보를 보냄
        messagingTemplate.convertAndSend("/topic/plans/" + planId + "/schedules", response);
    }


}
