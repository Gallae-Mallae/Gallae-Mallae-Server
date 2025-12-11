package com.practice.OAuth2.domain.plan.dto;

import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScheduleMoveRequest {
    private Integer newDay;       // 몇 일차로 옮겼는지
    private LocalTime newStartTime; // 몇 시로 옮겼는지
}
