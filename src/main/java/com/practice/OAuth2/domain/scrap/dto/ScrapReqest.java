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
        private String description;
        private String originalLink;
        private String content; // OGP 설명
        private String imageUrl;
    }

    // scrap 수정하기
    @Getter
    @NoArgsConstructor
    public static class UpdateScrap {
        private String title;
        private String description;
        private String originalLink;
        //private String imageUrl; // url바뀌면 서버가 자동으로 갱신
        //private String content; // 자동 갱신
    }

    // scrap folder 수정하기
    @Getter
    @NoArgsConstructor
    public static class UpdateScrapFolder {
        private String name;
        private String folderImageUrl;
    }
}
