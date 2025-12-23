package com.practice.OAuth2.domain.scrap.entity;

import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scraps")
public class Scrap extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Long scrapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private ScrapFolder scrapFolder;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content; // 웹페이지 설명(스크래핑으로 받아옴)

    @Column(columnDefinition = "TEXT")
    private String description; // 사용자가 적는 스크랩에 대한 메모

    @Column(name = "original_link")
    private String originalLink;

    @Column(name = "image_url")
    private String imageUrl;

    // 생성자 추가
    @Builder
    public Scrap(ScrapFolder scrapFolder, String title, String content, String description, String originalLink, String imageUrl){
        this.scrapFolder = scrapFolder;
        this.title = title;
        this.content = content;
        this.description = description;
        this.originalLink = originalLink;
        this.imageUrl = imageUrl;
    }

    // update Scrap
    public void updateScrap(String title, String description, String originalLink, String imageUrl, String content) {
        this.title = title;
        this.description = description;
        this.originalLink = originalLink;
        this.imageUrl = imageUrl;
        this.content = content;
    }
}
