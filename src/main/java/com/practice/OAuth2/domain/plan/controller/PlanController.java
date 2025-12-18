package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.PlanCreateRequest;
import com.practice.OAuth2.domain.plan.dto.PlanJoinRequest;
import com.practice.OAuth2.domain.plan.dto.PlanResponse;
import com.practice.OAuth2.domain.plan.dto.PlanUpdateRequest;
import com.practice.OAuth2.domain.plan.service.PlanService;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.security.UserPrincipal;
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
@RequestMapping("/api/plans")
public class PlanController {
    private final PlanService planService;

    // 여행 생성
    @PostMapping()
    public ResponseEntity<PlanResponse> createPlan(
            @RequestBody PlanCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
            ){

        PlanResponse response = planService.createPlan(request, principal.getId());
        return ResponseEntity.ok(response);
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
