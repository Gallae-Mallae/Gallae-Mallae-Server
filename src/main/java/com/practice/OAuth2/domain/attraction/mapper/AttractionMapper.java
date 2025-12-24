package com.practice.OAuth2.domain.attraction.mapper;

import com.practice.OAuth2.domain.attraction.dto.AttractionRequest;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse2;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AttractionMapper {

    // 1. 전체 개수 조회
    int countAttractions(AttractionRequest request);

    // 2. 클러스터링 조회
    List<AttractionResponse2> findClusteredAttractions(AttractionRequest request);

    // 3. 낱개 마커 조회
    List<AttractionResponse2> findRawAttractions(AttractionRequest request);

    // 테스트용
    List<AttractionResponse> findAllAttractions();

    //페이지네이션
    List<AttractionResponse2> findSidebarList(AttractionRequest request);

    // 인기 여행지 리스트 (조건 없음, 좋아요 순)
    List<AttractionResponse2> findPopularAttractions(AttractionRequest request);
}


