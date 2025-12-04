package com.practice.OAuth2.domain.scrap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class ScrapReqest {

    // scrap folder 만들기
    @Getter
    @NoArgsConstructor
    public static class CreateScrapFolder{
        private String name;
        private String description;
    }

    // scrap 만들기
    @Getter
    @NoArgsConstructor
    public static class CreateScrap {
        private String title;
        private String content;
        private String originLink;
        private String imageUrl;
    }
}
