package com.practice.OAuth2.domain.plan.dto;

import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import java.time.LocalTime;
import lombok.Getter;

@Getter
public class ScheduleBlockResponse {
    private Long blockId;
    private Long planId;      // Plan 전체 대신 ID만 보냄
    private AttractionResponse attraction; // Attraction도 DTO로 변환
    private Integer day;
    private String title;
    private LocalTime startTime;
    private LocalTime endTime;

    public ScheduleBlockResponse(ScheduleBlock block) {
        this.blockId = block.getBlockId();
        this.planId = block.getPlan().getPlanId();

        // Attraction이 있으면 DTO로 변환, 없으면(빈 블록) null
        if (block.getAttraction() != null) {
            this.attraction = new AttractionResponse(block.getAttraction());
        }

        this.day = block.getDay();
        this.title = block.getTitle();
        this.startTime = block.getStartTime();
        this.endTime = block.getEndTime();
    }
}
