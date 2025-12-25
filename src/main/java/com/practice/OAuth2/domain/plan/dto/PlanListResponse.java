package com.practice.OAuth2.domain.plan.dto;

import com.practice.OAuth2.domain.plan.entity.Plan;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PlanListResponse {
    private Long planId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String planImageUrl;
    private Integer isShared;

    public PlanListResponse(Plan plan, int memberCount) {
        this.planId = plan.getPlanId();
        this.title = plan.getTitle();
        this.startDate = plan.getStartDate();
        this.endDate = plan.getEndDate();
        this.planImageUrl = plan.getPlanImageUrl();
        this.isShared = (memberCount > 1) ? 2 : 1;
    }
}
