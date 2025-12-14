package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.entity.ConnFolderPlace;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Getter
public class PlaceFolderInfoResponse {
    private Integer attractionId;
    private String title;         // 장소명
    private String address;       // 주소 (addr1)
    private String imageUrl;      // 썸네일 이미지 (firstImage1)
    private Long likeCount;

    @Builder
    public PlaceFolderInfoResponse(Integer attractionId, String title, String address, String imageUrl, Long likeCount) {
        this.attractionId = attractionId;
        this.title = title;
        this.address = address;
        this.imageUrl = imageUrl;
        this.likeCount = likeCount;
    }

    // 중간테이블 엔티티객체의 FK로 바로 Attraction 연결
    public static PlaceFolderInfoResponse from(ConnFolderPlace conn) {
        return PlaceFolderInfoResponse.builder()
                .attractionId(conn.getAttraction().getAttrId())
                .title(conn.getAttraction().getTitle())
                .address(conn.getAttraction().getAddr1())
                .imageUrl(conn.getAttraction().getFirstImage1())
                .likeCount(conn.getAttraction().getLikeCount())
                .build();
    }
}
