package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data              // 1. Getter, Setter 다 만들어줌 (마이바티스가 데이터 넣을 때 필요)
@NoArgsConstructor // 2. 빈 생성자 만들어줌 (마이바티스가 객체 생성할 때 필요)
@AllArgsConstructor // 3. 모든 필드 생성자 만들어줌
public class AttractionResponse {
    private Integer attractionId;
    private String title;         // 장소명
    private String address;       // 주소 (addr1)
    private String imageUrl;      // 썸네일 이미지 (firstImage1)
    private Double latitude;      // BigDecimal -> Double 변환
    private Double longitude;

    public AttractionResponse(Attraction attraction) {
        this.attractionId = attraction.getAttrId();
        this.title = attraction.getTitle();
        this.address = attraction.getAddr1(); // 주로 addr1이 도로명/지번 주소

        // 이미지가 없을 경우 firstImage2를 쓸 수도 있지만, 일단 1번을 메인으로 사용
        this.imageUrl = attraction.getFirstImage1();

        // BigDecimal을 프론트에서 쓰기 편한 Double로 변환 (null 체크 포함)
        if (attraction.getLatitude() != null) {
            this.latitude = attraction.getLatitude().doubleValue();
        }
        if (attraction.getLongitude() != null) {
            this.longitude = attraction.getLongitude().doubleValue();
        }
    }
}
