package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.plan.dto.PlanCreateRequest;
import com.practice.OAuth2.domain.plan.dto.PlanResponse;
import com.practice.OAuth2.domain.plan.entity.Plan;
import com.practice.OAuth2.domain.plan.entity.PlanMember;
import com.practice.OAuth2.domain.plan.repository.PlanMemberRepository;
import com.practice.OAuth2.domain.plan.repository.PlanRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository planRepository;
    private final PlanMemberRepository planMemberRepository;
    private final UserRepository userRepository;

    // 여행 생성
    @Transactional
    public PlanResponse createPlan(PlanCreateRequest request, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 사용자"));

        Plan plan = request.toEntity();
        planRepository.save(plan);

        // 최초 등록
        PlanMember firstMember = PlanMember.builder()
                .plan(plan)
                .user(user)
                .build();

        // plan_Members 테이블에 이 사용자 추가
        planMemberRepository.save(firstMember);

        // inviteCode가 포함된 응답이 나감
        return new PlanResponse(plan);
    }

    // 여행 수정

    // 여행 삭제
}
