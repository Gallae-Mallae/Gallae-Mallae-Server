package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionResponse {
    private Integer attractionId;
    private String title;
    private String address;
    private String imageUrl;
    private Double latitude;
    private Double longitude;

    // 추가된 필드
    private Long viewCount;
    private Long likeCount;
    private String overview;
    private Integer contentTypeId;

    public AttractionResponse(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1();
        this.imageUrl = attraction.getFirstImage1();

        // 추가 필드 매핑
        this.viewCount = attraction.getViewCount();
        this.likeCount = attraction.getLikeCount();
        this.overview = attraction.getOverview();

        this.contentTypeId = attraction.getContentTypeId();

        // BigDecimal -> Double
        if (attraction.getLatitude() != null) {
            this.latitude = attraction.getLatitude().doubleValue();
        }
        if (attraction.getLongitude() != null) {
            this.longitude = attraction.getLongitude().doubleValue();
        }
    }
}