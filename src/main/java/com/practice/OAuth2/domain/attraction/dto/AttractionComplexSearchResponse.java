package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionComplexSearchResponse {
    private Integer attractionId;
    private String title;
    private String address;
    private String imageUrl;
    private Integer contentTypeId;
    private Long viewCount;
    private Long likeCount;
    private LocalDateTime createdAt;
    private String sidoName;
    private String gugunName;

    public AttractionComplexSearchResponse(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1();
        this.imageUrl = attraction.getFirstImage1();
        this.viewCount = attraction.getViewCount();
        this.likeCount = attraction.getLikeCount();
        this.createdAt = attraction.getCreatedAt();
        this.contentTypeId = attraction.getContentTypeId();
        this.sidoName = (attraction.getSido() != null) ? attraction.getSido().getSidoName() : null;
        this.gugunName = (attraction.getGugun() != null) ? attraction.getGugun().getGugunName() : null;
    }
}
