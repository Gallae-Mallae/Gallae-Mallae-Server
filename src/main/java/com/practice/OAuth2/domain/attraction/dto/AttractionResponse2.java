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

    // viewCount, likeCount, contentTypeId는 용량이 작고 유용하니 유지 추천!
    private Long viewCount;
    private Long likeCount;
    private Integer contentTypeId;



    public AttractionResponse2(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1();
        this.imageUrl = attraction.getFirstImage1();
        this.count = 1;

        this.viewCount = attraction.getViewCount();
        this.likeCount = attraction.getLikeCount();



        if (attraction.getContentType() != null) {
            this.contentTypeId = attraction.getContentType().getContentTypeId();
        }

        if (attraction.getLatitude() != null) {
            this.latitude = attraction.getLatitude().doubleValue();
        }
        if (attraction.getLongitude() != null) {
            this.longitude = attraction.getLongitude().doubleValue();
        }
    }
}