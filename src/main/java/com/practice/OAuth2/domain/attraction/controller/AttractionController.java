package com.practice.OAuth2.domain.attraction.controller;

import com.practice.OAuth2.domain.attraction.dto.AttractionRequest;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse2;
import com.practice.OAuth2.domain.attraction.service.AttractionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/map") //
    public ResponseEntity<List<AttractionResponse2>> getMapMarkers(
            @ModelAttribute AttractionRequest request
    ) {
        List<AttractionResponse2> result = attractionService.getMapMarkers(request);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/map/sidebar")
    public ResponseEntity<com.practice.OAuth2.domain.attraction.dto.AttractionSliceResponse> getSidebarList(
            @ModelAttribute AttractionRequest request
    ) {
        // 페이지네이션 로직 호출
        var result = attractionService.getSidebarList(request);
        return ResponseEntity.ok(result);
    }



    // MyBatis 연결 테스트용
    // 접속 주소: http://localhost:8080/api/attractions/map/mybatis-test
    @GetMapping("/map/mybatis-test")
    public ResponseEntity<List<AttractionResponse>> testMyBatis() {
        List<AttractionResponse> results = attractionService.getAttractionListTest();
        return ResponseEntity.ok(results);
    }





}
