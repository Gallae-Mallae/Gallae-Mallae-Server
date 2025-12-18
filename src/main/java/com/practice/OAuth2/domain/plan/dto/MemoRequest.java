package com.practice.OAuth2.domain.plan.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemoRequest {
    private Long blockId;
    private String content;
    private String linkUrl;
    private String type;
}