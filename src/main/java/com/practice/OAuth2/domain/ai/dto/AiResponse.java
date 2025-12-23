package com.practice.OAuth2.domain.ai.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {

    // AI가 답변한 텍스트 (예: "경주 불국사를 추천합니다...")
    private String aiMessage;

    // 추천된 장소들의 상세 정보 리스트 (프론트엔드 지도 표시용)
    private List<PlaceInfo> places;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceInfo {
        private Integer id;      // 장소 ID (attractionId)
        private String title;    // 장소명
        private String address;  // 주소
        private String image;    // 이미지 URL
    }
}