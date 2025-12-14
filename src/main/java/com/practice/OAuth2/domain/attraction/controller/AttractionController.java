package com.practice.OAuth2.domain.attraction.controller;

import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.attraction.service.AttractionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attractions")
public class AttractionController {
    private final AttractionService attractionService;

    // 장소 검색
    @GetMapping
    public ResponseEntity<List<AttractionResponse>> search(@RequestParam String keyword) {
        List<AttractionResponse> results = attractionService.searchAttractions(keyword);
        return ResponseEntity.ok(results);
    }




    // MyBatis 연결 테스트용
    // 접속 주소: http://localhost:8080/api/attractions/mybatis-test
    @GetMapping("/mybatis-test")
    public ResponseEntity<List<AttractionResponse>> testMyBatis() {
        List<AttractionResponse> results = attractionService.getAttractionListTest();
        return ResponseEntity.ok(results);
    }


}
