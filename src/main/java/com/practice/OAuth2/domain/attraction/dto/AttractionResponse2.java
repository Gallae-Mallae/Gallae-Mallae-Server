package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionResponse2 {

    private Integer attractionId;
    private Integer count;
    private String title;
    private Double latitude;
    private Double longitude;

    private String address;
    private String imageUrl;

    private Long viewCount;
    private Long likeCount;
    private Integer contentTypeId;

    // ==========================================
    // [추가] AI 분석 및 RAG용 필수 필드
    // ==========================================
    private String overview;        // 관광지 상세 설명 (DB의 overview 컬럼)
    private String contentTypeName; // 유형 이름 (DB의 contenttypes 테이블 조인)

    public AttractionResponse2(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1();
        this.imageUrl = attraction.getFirstImage1();
        this.count = 1;

        this.viewCount = attraction.getViewCount();
        this.likeCount = attraction.getLikeCount();

        // Entity에서 가져올 때도 매핑 (기존 로직 유지용)
        this.overview = attraction.getOverview();

        if (attraction.getContentType() != null) {
            this.contentTypeId = attraction.getContentType().getContentTypeId();
            this.contentTypeName = attraction.getContentType().getContentTypeName(); // 추가
        }

        if (attraction.getLatitude() != null) {
            this.latitude = attraction.getLatitude().doubleValue();
        }
        if (attraction.getLongitude() != null) {
            this.longitude = attraction.getLongitude().doubleValue();
        }
    }
}