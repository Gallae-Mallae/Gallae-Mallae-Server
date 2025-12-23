package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.ScheduleBlockResponse;
import com.practice.OAuth2.domain.plan.dto.ScheduleCreateRequest;
import com.practice.OAuth2.domain.plan.dto.ScheduleMoveRequest;
import com.practice.OAuth2.domain.plan.dto.ScheduleResizeRequest;
import com.practice.OAuth2.domain.plan.service.ScheduleService;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserRepository userRepository;

    // 스케줄 블록 생성
    @PostMapping("/{planId}")
    public ResponseEntity<Void> createBlock(
            @PathVariable Long planId,
            @RequestBody ScheduleCreateRequest request
    ) {
        scheduleService.createScheduleBlock(planId, request);
        return ResponseEntity.ok().build();
    }

    // 스케줄 블럭 조회
    @GetMapping("/{planId}")
    public ResponseEntity<List<ScheduleBlockResponse>> getSchedules(
            @PathVariable Long planId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return ResponseEntity.ok(scheduleService.getSchedules(user.getUserId(), planId));
    }

    // 특정day 스케줄 블럭 조회
    @GetMapping("/{planId}/days/{day}")
    public ResponseEntity<List<ScheduleBlockResponse>> getSchedulesByDay(
            @PathVariable Long planId,
            @PathVariable Integer day,
            @AuthenticationPrincipal UserDetails userDetails // 또는 UserPrincipal
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        return ResponseEntity.ok(scheduleService.getSchedulesByDay(user.getUserId(), planId, day));
    }

    // 스케줄 블록 시간 늘리기
    @PatchMapping("/{blockId}/resize")
    public ResponseEntity<Void> resizeBlock(
            @PathVariable Long blockId,
            @RequestBody ScheduleResizeRequest request
    ) {
        scheduleService.resizeScheduleBlock(blockId, request.getNewEndTime());
        return ResponseEntity.ok().build();
    }

    // 스케줄 블록 이동
    @PatchMapping("/{blockId}/position")
    public ResponseEntity<Void> moveScheduleBlock(
            @PathVariable Long blockId,
            @RequestBody ScheduleMoveRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        Long userId = user.getUserId();

        // DB 수정 -> STOMP 전송
        scheduleService.moveScheduleBlock(
                userId,
                blockId,
                request.getNewDay(),
                request.getNewStartTime()
        );

        return ResponseEntity.ok().build();
    }

    // 스케줄 블록 삭제
    @DeleteMapping("/{blockId}")
    public ResponseEntity<Void> deleteBlock(
            @PathVariable Long blockId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        Long userId = user.getUserId();

        scheduleService.deleteScheduleBlock(userId, blockId);
        return ResponseEntity.ok().build();
    }
}
