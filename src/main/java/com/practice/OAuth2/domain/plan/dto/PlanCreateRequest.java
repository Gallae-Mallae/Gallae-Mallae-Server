package com.practice.OAuth2.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.practice.OAuth2.domain.plan.entity.Plan;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@NoArgsConstructor
// 사용자가 여행 만들거나, 입장할때
public class PlanCreateRequest {

    @NonNull
    private String title;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

//    // DTO -> Entity 변환 메서드 (서비스 로직 단축용)
//    public Plan toEntity() {
//        return Plan.builder()
//                .title(this.title)
//                .startDate(this.startDate)
//                .endDate(this.endDate)
//                .build();
//        // inviteCode는 엔티티 생성자(Builder) 안에서 자동 생성
//    }
}
