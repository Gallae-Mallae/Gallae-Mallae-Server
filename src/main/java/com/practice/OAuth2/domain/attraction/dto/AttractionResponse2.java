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

    private String overview;
    private String contentTypeName;

    // [샌드위치 전략용 추가]
    private String sidoName;
    private String gugunName;

    public AttractionResponse2(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1();
        this.imageUrl = attraction.getFirstImage1();
        this.count = 1;
        this.viewCount = attraction.getViewCount();
        this.likeCount = attraction.getLikeCount();
        this.overview = attraction.getOverview();

        if (attraction.getContentType() != null) {
            this.contentTypeId = attraction.getContentType().getContentTypeId();
            this.contentTypeName = attraction.getContentType().getContentTypeName();
        }

        // [추가] 엔티티 관계를 통해 시도/구군 이름 가져오기
        if (attraction.getGugun() != null) {
            this.gugunName = attraction.getGugun().getGugunName();
            if (attraction.getGugun().getSido() != null) {
                this.sidoName = attraction.getGugun().getSido().getSidoName();
            }
        }

        if (attraction.getLatitude() != null) {
            this.latitude = attraction.getLatitude().doubleValue();
        }
        if (attraction.getLongitude() != null) {
            this.longitude = attraction.getLongitude().doubleValue();
        }
    }
}