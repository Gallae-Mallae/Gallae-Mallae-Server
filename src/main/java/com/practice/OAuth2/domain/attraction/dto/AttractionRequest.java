package com.practice.OAuth2.domain.attraction.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionRequest {

    // (화면 좌표 & 줌 레벨)
    private Integer zoomLevel;      // 1~13
    private Double southWestLat;
    private Double southWestLng;
    private Double northEastLat;
    private Double northEastLng;


    private Integer sido;            // 시/도 코드
    private Integer guguns;         // 구/군 코드
    private Integer contenttype;    // 관광지 타입
    private String keyword;         // 검색어


    // [추가] 페이지네이션 필드
    private Integer page = 0;  // 기본값 0페이지
    private Integer size = 20; // 기본값 20개




    // XML에서 #{precision}을 쓰면 이 메서드가 실행
    public Integer getPrecision() {
        if (this.zoomLevel == null) return 5; // 기본값 방어


        if (this.zoomLevel >= 11){
            return 3;
        }else if(this.zoomLevel >= 8){
            return 4;
        }else if(this.zoomLevel >= 6){
            return 6;
        }else{
            return 7;
        }
    }



    // [추가] 페이지네이션
    public int getOffset() {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 20;
        return page * size;
    }

    public int getLimit() {
        if (size == null) size = 20;
        return size + 1;
    }
}