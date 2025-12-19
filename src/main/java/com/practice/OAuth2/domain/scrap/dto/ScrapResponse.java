package com.practice.OAuth2.domain.scrap.dto;


import com.practice.OAuth2.domain.scrap.entity.Scrap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ScrapResponse {
    private Long scrapId;
    private String title;
    private String content;
    private String description;
    private String originalLink;
    private String imageUrl;

    // DTO 변환 메서드 필요
    public static ScrapResponse from(Scrap scrap){
        return ScrapResponse.builder()
                .scrapId(scrap.getScrapId())
                .title(scrap.getTitle())
                .content(scrap.getContent())
                .description(scrap.getDescription())
                .originalLink(scrap.getOriginalLink())
                .imageUrl(scrap.getImageUrl())
                .build();
    }
}
