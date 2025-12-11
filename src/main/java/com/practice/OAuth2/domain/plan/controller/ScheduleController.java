package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.ScheduleCreateRequest;
import com.practice.OAuth2.domain.plan.dto.ScheduleMoveRequest;
import com.practice.OAuth2.domain.plan.dto.ScheduleResizeRequest;
import com.practice.OAuth2.domain.plan.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 스케줄 블록 생성
    @PostMapping("/{planId}")
    public ResponseEntity<Void> createBlock(
            @PathVariable Long planId,
            @RequestBody ScheduleCreateRequest request
    ) {
        scheduleService.createScheduleBlock(planId, request);
        return ResponseEntity.ok().build();
    }

    // 스케줄 시간 늘리기
    @PatchMapping("/{blockId}/resize")
    public ResponseEntity<Void> resizeBlock(
            @PathVariable Long blockId,
            @RequestBody ScheduleResizeRequest request
    ) {
        scheduleService.resizeScheduleBlock(blockId, request.getNewEndTime());
        return ResponseEntity.ok().build();
    }

    // 스케줄 이동
    @PatchMapping("/{blockId}/position")
    public ResponseEntity<Void> moveScheduleBlock(
            @PathVariable Long blockId,
            @RequestBody ScheduleMoveRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        // DB 수정 -> STOMP 전송
        scheduleService.moveScheduleBlock(
                userId,
                blockId,
                request.getNewDay(),
                request.getNewStartTime()
        );

        return ResponseEntity.ok().build();
    }
}
