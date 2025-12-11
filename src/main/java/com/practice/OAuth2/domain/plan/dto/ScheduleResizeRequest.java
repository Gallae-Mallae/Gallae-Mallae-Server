package com.practice.OAuth2.domain.plan.dto;

import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScheduleResizeRequest {
    private LocalTime newEndTime;
}
