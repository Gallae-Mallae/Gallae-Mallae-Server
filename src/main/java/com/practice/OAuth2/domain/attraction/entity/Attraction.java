package com.practice.OAuth2.domain.attraction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import jdk.jfr.ContentType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "attractions")
public class Attraction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attr_id")
    private Integer attrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_type_id")
    private ContentType contentType;

    @Column(length = 500)
    private String title;

    @Column(name = "sido_code")
    private Integer sidoCode;

    @Column(name = "gugun_code")
    private Integer gugunCode;

    @Column(name = "first_image1", length = 100)
    private String firstImage1;

    @Column(name = "first_image2", length = 100)
    private String firstImage2;

    @Column(name = "map_level")
    private Integer mapLevel;

    @Column(precision = 20, scale = 17)
    private BigDecimal latitude;

    @Column(precision = 20, scale = 17)
    private BigDecimal longitude;

    @Column(length = 20)
    private String tel;

    @Column(length = 100)
    private String addr1;

    @Column(length = 100)
    private String addr2;

    @Column(length = 1000)
    private String homepage;

    @Column(length = 10000)
    private String overview;

    @Column(length = 12)
    private String geohash;

    @Column(name = "view_count", columnDefinition = "int DEFAULT 0")
    private Integer viewCount;

    @Column(name = "like_count", columnDefinition = "int DEFAULT 0")
    private Integer likeCount;
}
