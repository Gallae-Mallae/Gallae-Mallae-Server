package com.practice.OAuth2.domain.attraction.service;

import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
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

    //  MyBatis 테스트용 메서드
    public List<AttractionResponse> getAttractionListTest() {

        return attractionMapper.findAllAttractions();
    }


}
