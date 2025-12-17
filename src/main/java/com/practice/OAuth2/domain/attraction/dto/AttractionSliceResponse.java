package com.practice.OAuth2.domain.attraction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttractionSliceResponse {
    private List<AttractionResponse2> attractions; // 목록 데이터
    private boolean hasNext;                       // 다음 페이지 존재 여부
    private int nowPage;                           // 현재 페이지 번호
}