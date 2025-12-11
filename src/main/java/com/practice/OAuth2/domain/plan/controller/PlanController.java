package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.PlanCreateRequest;
import com.practice.OAuth2.domain.plan.dto.PlanJoinRequest;
import com.practice.OAuth2.domain.plan.dto.PlanResponse;
import com.practice.OAuth2.domain.plan.service.PlanService;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
        Long userId = Long.parseLong(principal.getUsername());

        PlanResponse response = planService.createPlan(request, userId);
        return ResponseEntity.ok(response);
    }

    // 초대 코드로 입장
    @PostMapping("/join")
    public ResponseEntity<Long> joinPlan(
            @RequestBody PlanJoinRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());

        Long planId = planService.joinPlan(request, userId);

        // 성공 시 입장한 방의 ID 반환
        return ResponseEntity.ok(planId);
    }
}
