package com.practice.OAuth2.domain.plan.dto;

import com.practice.OAuth2.domain.plan.entity.PlanMember;
import lombok.Getter;

@Getter
// 참여자 정보
public class PlanMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickname;
    private String profileImageUrl;

    public PlanMemberResponse(PlanMember planMember) {
        this.memberId = planMember.getMemberId();
        // PlanMember -> User 접근
        this.userId = planMember.getUser().getUserId();
        this.nickname = planMember.getUser().getNickname(); // User 엔티티 필드명에 맞게 수정
        this.profileImageUrl = planMember.getUser().getProfileImageUrl();
    }
}
