package com.practice.OAuth2.domain.attraction.mapper;

import com.practice.OAuth2.domain.attraction.dto.AttractionRequest;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse2;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

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

    // [AI 기능 추가 1] RAG 데이터 학습용 (Offset 페이징)
    List<AttractionResponse2> findRagData(@Param("limit") int limit, @Param("offset") int offset);

    // [AI 기능 추가 2] 채팅 시 ID 리스트로 상세 정보 조회
    List<AttractionResponse2> findAllByIds(@Param("ids") List<Integer> ids);
}


