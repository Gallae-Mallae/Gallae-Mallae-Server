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
}
