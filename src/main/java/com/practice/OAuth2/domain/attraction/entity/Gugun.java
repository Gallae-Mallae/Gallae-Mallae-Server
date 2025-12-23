package com.practice.OAuth2.domain.attraction.entity;

import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "guguns")
public class Gugun extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guguns_id")
    private Integer gugunsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sido_code", referencedColumnName = "sido_code")
    private Sido sido;

    @Column(name = "gugun_code", nullable = false, unique=true)
    private Integer gugunCode;

    @Column(name = "gugun_name", length = 20)
    private String gugunName;
}
