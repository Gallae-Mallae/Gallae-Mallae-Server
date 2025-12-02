package com.practice.OAuth2.domain.attraction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "guguns")
public class Gugun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guguns_id")
    private Integer gugunsId;

    @Column(name = "sido_code", nullable = false)
    private Integer sidoCode;

    @Column(name = "gugun_code", nullable = false)
    private Integer gugunCode;

    @Column(name = "gugun_name", length = 20)
    private String gugunName;
}
