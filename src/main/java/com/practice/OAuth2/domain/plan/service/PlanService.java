package com.practice.OAuth2.domain.plan.service;

import com.practice.OAuth2.domain.plan.dto.*;
import com.practice.OAuth2.domain.plan.entity.Plan;
import com.practice.OAuth2.domain.plan.entity.PlanMember;
import com.practice.OAuth2.domain.plan.repository.PlanMemberRepository;
import com.practice.OAuth2.domain.plan.repository.PlanRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private static final String SPRING_IMAGE = "https://gm-public-bucket.s3.ap-northeast-2.amazonaws.com/spring.png";
    private static final String SUMMER_IMAGE = "https://gm-public-bucket.s3.ap-northeast-2.amazonaws.com/summer.png";
    private static final String AUTUMN_IMAGE = "https://gm-public-bucket.s3.ap-northeast-2.amazonaws.com/autumn.png";
    private static final String WINTER_IMAGE = "https://gm-public-bucket.s3.ap-northeast-2.amazonaws.com/winter.png";

    // 여행 생성
    @Transactional
    public PlanResponse createPlan(PlanCreateRequest request, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 사용자"));

        String seasonalImageUrl = getSeasonalImageUrl(request.getStartDate());

        Plan plan = Plan.builder()
                .title(request.getTitle())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .planImageUrl(seasonalImageUrl) // 선정된 이미지 저장
                .build();
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

    private String getSeasonalImageUrl(LocalDate startDate) {
        int month = startDate.getMonthValue(); // 1~12

        if (month >= 3 && month <= 5) {
            return SPRING_IMAGE; // 3, 4, 5월 -> 봄
        } else if (month >= 6 && month <= 8) {
            return SUMMER_IMAGE; // 6, 7, 8월 -> 여름
        } else if (month >= 9 && month <= 11) {
            return AUTUMN_IMAGE; // 9, 10, 11월 -> 가을
        } else {
            return WINTER_IMAGE; // 12, 1, 2월 -> 겨울
        }
    }

    // 여행 목록 조회
    @Transactional(readOnly = true)
    public List<PlanListResponse> getMyPlans(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        // 내가 참여 중인 PlanMember 리스트 조회 -> Plan 정보 추출 -> DTO 변환
        // (PlanMemberRepository에 해당 메서드가 정의되어 있어야 함)
        return planMemberRepository.findByUser_UserIdAndLeftAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(pm -> new PlanListResponse(pm.getPlan()))
                .collect(Collectors.toList());
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
        sendStompMessage(plan.getPlanId(), "MEMBER_JOINED", responseDto);

        return plan.getPlanId();
    }

    // 여행 참여자 목록 조회
    @Transactional(readOnly = true)
    public List<PlanMemberResponse> getPlanMembers(Long userId, Long planId) {
        // 권한 체크
        boolean isMember = planMemberRepository.existsByPlan_PlanIdAndUser_UserIdAndLeftAtIsNull(planId, userId);
        if (!isMember) {
            throw new IllegalArgumentException("해당 여행의 멤버 목록을 조회할 권한이 없습니다.");
        }

        // 멤버 목록 조회 및 DTO 변환
        return planMemberRepository.findByPlan_PlanIdAndLeftAtIsNull(planId).stream()
                .map(PlanMemberResponse::new)
                .collect(Collectors.toList());
    }

    // 여행 수정 (제목, 기간)
    @Transactional
    public void updatePlan(Long planId, PlanUpdateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

        // 권한 체크

        // 기간 변경시 사진도 변경
        String newSeasonalImageUrl = getSeasonalImageUrl(request.getStartDate());

        // 데이터 수정
        plan.update(request.getTitle(), request.getStartDate(), request.getEndDate(), newSeasonalImageUrl);

        // [STOMP] PLAN_UPDATED 알림 전송
        // 변경된 Plan 정보를 모두에게 발행
        sendStompMessage(planId, "PLAN_UPDATED", new PlanResponse(plan));
    }

    @Transactional
    public void deletePlan(Long planId, Long userId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

        boolean isMember = planMemberRepository.existsByPlan_PlanIdAndUser_UserIdAndLeftAtIsNull(planId, userId);
        if (!isMember) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        planRepository.delete(plan);

        sendStompMessage(planId, "PLAN_DELETED", planId);
    }

    private void sendStompMessage(Long planId, String eventType, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("event", eventType);
        message.put("data", data);
        messagingTemplate.convertAndSend("/topic/plans/" + planId, message);
    }
}
