package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import com.practice.OAuth2.domain.plan.repository.ScheduleBlockRepository;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
// 스케줄 블록을 옮겼을 때, publish 하기
public class ScheduleService {

    private final ScheduleBlockRepository scheduleBlockRepository;

    private final SimpMessagingTemplate messagingTemplate;

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

        // 변경된 블록 정보만 보내거나, 해당 날짜의 전체 리스트를 보내서 덮어씌우게 함
        // "UPDATE"라는 신호와 함께 변경된 블록 정보를 보냄
        messagingTemplate.convertAndSend("/topic/plans/" + planId + "/schedules", block);
    }


}
