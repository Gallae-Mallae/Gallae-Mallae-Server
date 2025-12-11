package com.practice.OAuth2.domain.plan.dto;

import com.practice.OAuth2.domain.plan.entity.Plan;
import java.time.LocalDate;
import lombok.Getter;

@Getter
// 여행 정보 조회시 프론트엔드에 전송
public class PlanResponse {
    private Long planId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String planImageUrl;
    private String inviteCode; // 친구 초대용 코드

    // Entity -> DTO 생성자
    public PlanResponse(Plan plan) {
        this.planId = plan.getPlanId();
        this.title = plan.getTitle();
        this.startDate = plan.getStartDate();
        this.endDate = plan.getEndDate();
        this.planImageUrl = plan.getPlanImageUrl();
        this.inviteCode = plan.getInviteCode();
    }
}
