package com.practice.OAuth2.domain.attraction.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionRequest {

    // 1. 필수 파라미터 (화면 좌표 & 줌 레벨)
    private Integer zoomLevel;      // 1~13
    private Double southWestLat;
    private Double southWestLng;
    private Double northEastLat;
    private Double northEastLng;

    // 2. 선택 파라미터 (필터링)
    private Integer sido;           // 시/도 코드
    private Integer guguns;         // 구/군 코드
    private Integer contenttype;    // 관광지 타입
    private String keyword;         // 검색어

    // 3. [중요] MyBatis가 호출할 로직 (줌 레벨 -> Geohash 자릿수 변환)
    // XML에서 #{precision}을 쓰면 이 메서드가 실행됩니다.
    public Integer getPrecision() {
        if (this.zoomLevel == null) return 5; // 기본값 방어

        // 숫자가 클수록 넓은 화면(13) -> 5자리 (동네)
        // 숫자가 작을수록 좁은 화면(1) -> 6자리 (블록)
        if (this.zoomLevel >= 8) {
            return 3;
        } else {
            return 6;
        }
    }
}