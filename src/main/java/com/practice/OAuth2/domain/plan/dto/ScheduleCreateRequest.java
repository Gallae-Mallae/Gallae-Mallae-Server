package com.practice.OAuth2.domain.plan.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ScheduleCreateRequest {
    private Integer attractionId;
    private Integer day;
    private String title;
    private LocalTime startTime;
}
