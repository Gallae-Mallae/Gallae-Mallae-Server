package com.practice.OAuth2.domain.attraction.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class AttractionComplexSearchRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime date;

    private Integer contentTypeId;

    private Long minLikeCount;

    private int page = 0;

    private int size = 20;
}
