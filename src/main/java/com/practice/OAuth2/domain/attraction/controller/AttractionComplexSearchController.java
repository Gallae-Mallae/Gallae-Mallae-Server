package com.practice.OAuth2.domain.attraction.controller;

import com.practice.OAuth2.domain.attraction.dto.AttractionComplexSearchRequest;
import com.practice.OAuth2.domain.attraction.dto.AttractionComplexSearchResponse;
import com.practice.OAuth2.domain.attraction.service.AttractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attractions")
public class AttractionComplexSearchController {

    private final AttractionService attractionService;

    // 복합 조건 관광지 검색 (인덱스 미적용 - 풀 테이블 스캔 발생)
    // GET /api/chapter3/attractions/complex-search?date=2024-01-01T00:00:00&contentTypeId=12&minViewCount=100&page=0&size=20
    @GetMapping("/complex-search")
    public ResponseEntity<Page<AttractionComplexSearchResponse>> complexSearch(
            @ModelAttribute AttractionComplexSearchRequest request
    ) {
        Page<AttractionComplexSearchResponse> result = attractionService.complexSearch(request);
        return ResponseEntity.ok(result);
    }
}
