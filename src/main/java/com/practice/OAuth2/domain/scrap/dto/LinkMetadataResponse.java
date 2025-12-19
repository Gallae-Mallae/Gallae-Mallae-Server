package com.practice.OAuth2.domain.scrap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LinkMetadataResponse {
    private String title;       // og:title
    private String url;         // 원본 링크
    private String imageUrl;    // og:image -> 썸네일로 사용
    private String description; // og:description
}