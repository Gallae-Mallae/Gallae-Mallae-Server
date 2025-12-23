package com.practice.OAuth2.domain.plan.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ScheduleResizeRequest {
    private LocalTime newEndTime;
}
