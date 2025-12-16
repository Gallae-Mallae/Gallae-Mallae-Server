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

        // 1. [유효성 검사] 4개 중 최소 1개는 값이 있어야 함!
        if (!hasAtLeastOneCondition(request)) {
            throw new IllegalArgumentException("검색 조건(시도, 구군, 검색어, 컨텐츠타입) 중 하나는 필수입니다.");
        }

        // 2. 전체 개수 조회
        int totalCount = attractionMapper.countAttractions(request);

        // 3. 개수에 따른 로직 분기 (기존 코드)
        if (totalCount < 200) {
            return attractionMapper.findRawAttractions(request);
        } else {
            return attractionMapper.findClusteredAttractions(request);
        }
    }

    private boolean hasAtLeastOneCondition(AttractionRequest req) {

        // 1. int 타입들 (sido, guguns, contenttype) -> Integer로 가정하고 null 체크
        // 만약 DTO가 원시타입 int라면 "req.getSido() != 0" 으로 고쳐야 합니다.
        boolean hasSido = req.getSido() != null;
        boolean hasGuguns = req.getGuguns() != null;
        boolean hasContentType = req.getContenttype() != null;

        // 2. String 타입 (keyword) -> null 아니고, 빈 문자열("") 아니고, 공백(" ")도 아닌지 체크
        boolean hasKeyword = req.getKeyword() != null && !req.getKeyword().trim().isEmpty();

        // 3. 넷 중 하나라도 true면 통과 (OR 연산)
        return hasSido || hasGuguns || hasContentType || hasKeyword;
    }

    //  MyBatis 테스트용 메서드
    public List<AttractionResponse> getAttractionListTest() {

        return attractionMapper.findAllAttractions();
    }




}
