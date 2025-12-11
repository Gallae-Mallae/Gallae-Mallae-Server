package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.PlanCreateRequest;
import com.practice.OAuth2.domain.plan.dto.PlanJoinRequest;
import com.practice.OAuth2.domain.plan.dto.PlanResponse;
import com.practice.OAuth2.domain.plan.service.PlanService;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // 여행 생성
    @PostMapping()
    public ResponseEntity<PlanResponse> createPlan(
            @RequestBody PlanCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
            ){
        String email = principal.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        Long userId = user.getUserId();

        PlanResponse response = planService.createPlan(request, userId);
        return ResponseEntity.ok(response);
    }

    // 초대 코드로 입장
    @PostMapping("/join")
    public ResponseEntity<Long> joinPlan(
            @RequestBody PlanJoinRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        Long userId = user.getUserId();

        Long planId = planService.joinPlan(request, userId);

        // 성공 시 입장한 방의 ID 반환
        return ResponseEntity.ok(planId);
    }
}
