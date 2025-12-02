package com.practice.OAuth2.domain.attraction.entity;

import com.practice.OAuth2.global.common.BaseEntity;
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
@Table(name = "sidos")
public class Sido extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sidos_id")
    private Integer sidosId;

    @Column(name = "sido_code", unique = true, nullable = false)
    private Integer sidoCode;

    @Column(name = "sido_name", length = 20)
    private String sidoName;
}
