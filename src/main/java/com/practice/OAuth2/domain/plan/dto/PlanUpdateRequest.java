package com.practice.OAuth2.domain.plan.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanUpdateRequest {
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}