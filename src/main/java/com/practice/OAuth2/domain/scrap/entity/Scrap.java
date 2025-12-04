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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
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
    private String content;

    @Column(name = "original_link")
    private String originalLink;

    @Column(name = "image_url")
    private String imageUrl;

    // 생성자 추가
    @Builder
    public Scrap(ScrapFolder scrapFolder, String title, String content, String originalLink, String imageUrl){
        this.scrapFolder = scrapFolder;
        this.title = title;
        this.content = content;
        this.originalLink = originalLink;
        this.imageUrl = imageUrl;
    }
}
