package com.practice.OAuth2.domain.scrap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class ScrapReqest {

    // scrap folder 만들기
    @Getter
    @NoArgsConstructor
    public static class CreateScrapFolder{
        private String name;
        private String folderImageUrl;
    }

    // scrap 만들기
    @Getter
    @NoArgsConstructor
    public static class CreateScrap {
        private String title;
        private String content;
        private String description;
        private String originalLink;
        private String imageUrl;
    }

    // scrap 수정하기
    @Getter
    @NoArgsConstructor
    public static class UpdateScrap {
        private String title;
        private String content;
        private String description;
        private String originalLink;
        private String imageUrl;
    }

    // scrap folder 수정하기
    @Getter
    @NoArgsConstructor
    public static class UpdateScrapFolder {
        private String name;
        private String folderImageUrl;
    }
}
