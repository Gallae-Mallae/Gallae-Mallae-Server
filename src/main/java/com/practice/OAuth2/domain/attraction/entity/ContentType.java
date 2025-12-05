package com.practice.OAuth2.domain.attraction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "contenttypes")
public class ContentType {
    @Id
    @Column(name = "content_type_id")
    private Integer contentTypeId;

    @Column(name = "content_type_name", length = 45)
    private String contentTypeName;
}
