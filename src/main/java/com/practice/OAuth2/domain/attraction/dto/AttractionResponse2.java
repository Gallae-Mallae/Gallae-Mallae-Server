package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionResponse2 {

    private Integer attractionId; // 클러스터면 0
    private Integer count;        // 1이면 마커, 2이상이면 클러스터
    private String title;
    private Double latitude;
    private Double longitude;

    // 상세 정보 (마커일 때만 존재)
    private String address;
    private String imageUrl;

    // Entity -> DTO 변환용 생성자 (낱개 마커용)
    public AttractionResponse2(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1();
        this.imageUrl = attraction.getFirstImage1();
        this.count = 1; // 낱개는 무조건 1

        if (attraction.getLatitude() != null) {
            this.latitude = attraction.getLatitude().doubleValue();
        }
        if (attraction.getLongitude() != null) {
            this.longitude = attraction.getLongitude().doubleValue();
        }
    }
}