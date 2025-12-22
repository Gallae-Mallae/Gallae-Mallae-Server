package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.*;
import com.practice.OAuth2.domain.plan.service.PlanService;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;
    private final UserRepository userRepository;

    // 여행 생성
    @PostMapping()
    public ResponseEntity<PlanResponse> createPlan(
            @RequestBody PlanCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
            ){

        PlanResponse response = planService.createPlan(request, principal.getId());
        return ResponseEntity.ok(response);
    }

    // 여행 목록 조회
    @GetMapping()
    public ResponseEntity<List<PlanListResponse>> getMyPlans(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(planService.getMyPlans(principal.getId()));
    }

    // 초대 코드로 입장
    @PostMapping("/join")
    public ResponseEntity<Long> joinPlan(
            @RequestBody PlanJoinRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long planId = planService.joinPlan(request, principal.getId());

        // 성공 시 입장한 방의 ID 반환
        return ResponseEntity.ok(planId);
    }

    // 여행 정보 수정 (제목, 날짜)
    @PatchMapping("/{planId}")
    public ResponseEntity<Void> updatePlan(
            @PathVariable Long planId,
            @RequestBody PlanUpdateRequest request
    ) {
        planService.updatePlan(planId, request);
        return ResponseEntity.ok().build();
    }
}
