package com.practice.OAuth2.domain.attraction.service;

import com.practice.OAuth2.domain.attraction.dto.AttractionRequest;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse2;
import com.practice.OAuth2.domain.attraction.mapper.AttractionMapper;
import com.practice.OAuth2.domain.attraction.repository.AttractionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttractionService {
    private final AttractionRepository attractionRepository;

    private final AttractionMapper attractionMapper;

    public List<AttractionResponse> searchAttractions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of(); // 검색어 없으면 빈 리스트
        }

        return attractionRepository.findByTitleContaining(keyword).stream()
                .map(AttractionResponse::new)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<AttractionResponse2> getMapMarkers(AttractionRequest request) {

        // 1. 현재 화면(Viewport) 안에 데이터가 총 몇 개인지 확인
        int totalCount = attractionMapper.countAttractions(request);

        // 2. 전략 선택 (Strategy Pattern)
        if (totalCount < 50) {
            // [CASE A] 50개 미만 -> 낱개 조회 (사진, 주소 포함)
            // (안전장치 LIMIT 100 걸려있음)
            return attractionMapper.findRawAttractions(request);
        } else {
            // [CASE B] 50개 이상 -> 클러스터링 조회 (껍데기만, 개수 위주)
            // (request.getPrecision()이 내부적으로 계산됨)
            return attractionMapper.findClusteredAttractions(request);
        }
    }




    //  MyBatis 테스트용 메서드
    public List<AttractionResponse> getAttractionListTest() {

        return attractionMapper.findAllAttractions();
    }




}
