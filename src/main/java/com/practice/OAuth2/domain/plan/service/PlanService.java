package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.plan.dto.PlanCreateRequest;
import com.practice.OAuth2.domain.plan.dto.PlanJoinRequest;
import com.practice.OAuth2.domain.plan.dto.PlanMemberResponse;
import com.practice.OAuth2.domain.plan.dto.PlanResponse;
import com.practice.OAuth2.domain.plan.entity.Plan;
import com.practice.OAuth2.domain.plan.entity.PlanMember;
import com.practice.OAuth2.domain.plan.repository.PlanMemberRepository;
import com.practice.OAuth2.domain.plan.repository.PlanRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository planRepository;
    private final PlanMemberRepository planMemberRepository;
    private final UserRepository userRepository;

    // STOMP 메시지 전송용 (드래그 앤 드롭에 쓰임)
    private final SimpMessagingTemplate messagingTemplate;

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

    // 친구 초대
    // db 업데이트 + 실시간 입장 알림 전송
    @Transactional
    public Long joinPlan(PlanJoinRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Plan plan = planRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        // 이 여행 멤버인지 확인
        Optional<PlanMember> existingMember = planMemberRepository.findByPlanAndUser(plan, user);

        PlanMember planMember;
        if (existingMember.isPresent()) {
            planMember = existingMember.get();
            // 이미 참여중이면
            if (planMember.getLeftAt() == null) {
                throw new IllegalStateException("이미 참여 중인 여행입니다.");
            }
            // 나갔던 멤버면 재입장 (leftAt -> null)
            planMember.reJoin();
        } else {
            // 새로 들어온 경우
            planMember = PlanMember.builder()
                    .plan(plan)
                    .user(user)
                    .build();
            planMemberRepository.save(planMember);
        }

        // STOMP 알림 전송 (화면 갱신해야 하니까)
        PlanMemberResponse responseDto = new PlanMemberResponse(planMember);
        messagingTemplate.convertAndSend("/topic/plans/" + plan.getPlanId(), responseDto);

        return plan.getPlanId();
    }

    // 여행 수정

    // 여행 삭제
}
