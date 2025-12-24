package com.practice.OAuth2.domain.attraction.service;

import com.practice.OAuth2.domain.attraction.dto.AttractionRequest;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse2;
import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.mapper.AttractionMapper;
import com.practice.OAuth2.domain.attraction.repository.AttractionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
@Transactional
public class AttractionService {
    private final AttractionRepository attractionRepository;
    private final AttractionMapper attractionMapper;
    private final com.practice.OAuth2.domain.attraction.repository.PlaceLikeRepository placeLikeRepository;
    private final com.practice.OAuth2.domain.user.repository.UserRepository userRepository;

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
    //검색 조건 유효성 검사 메서드
    private boolean hasAtLeastOneCondition(AttractionRequest req) {
        boolean hasSido = req.getSido() != null;
        boolean hasGuguns = req.getGuguns() != null;
        boolean hasContentType = req.getContenttype() != null;

        boolean hasKeyword = req.getKeyword() != null && !req.getKeyword().trim().isEmpty();

        return hasSido || hasGuguns || hasContentType || hasKeyword;
    }



    //사이드바 리스트
    @Transactional(readOnly = true)
    public com.practice.OAuth2.domain.attraction.dto.AttractionSliceResponse getSidebarList(AttractionRequest request) {

        // 1. 유효성 검사 (검색 조건 없으면 빈 리스트 반환)
        if (!hasAtLeastOneCondition(request)) {
            // 조건이 없으면 빈 결과 반환
            return new com.practice.OAuth2.domain.attraction.dto.AttractionSliceResponse(
                    java.util.Collections.emptyList(), false, request.getPage());
        }

        // 2. DB 조회 (요청 사이즈보다 1개 더 가져오도록 XML에서 설정)
        List<AttractionResponse2> result = attractionMapper.findSidebarList(request);

        // 3. hasNext 판단 로직
        boolean hasNext = false;
        if (result.size() > request.getSize()) {
            hasNext = true;
            result.remove(result.size() - 1); // 확인용으로 가져온 마지막 1개 제거
        }

        // 4. 결과 반환
        return new com.practice.OAuth2.domain.attraction.dto.AttractionSliceResponse(
                result, hasNext, request.getPage());
    }

    @Transactional(readOnly = true)
    public com.practice.OAuth2.domain.attraction.dto.AttractionSliceResponse getPopularAttractions(AttractionRequest request) {
        // 1. DB 조회 (요청 사이즈보다 1개 더 가져오도록 XML에서 설정 - AttractionRequest.getLimit() 확인 필요)
        // AttractionRequest.getLimit()가 size + 1을 반환하므로 findPopularAttractions 쿼리에서 LIMIT #{limit} 사용시 자동으로 +1개 가져옴
        List<AttractionResponse2> result = attractionMapper.findPopularAttractions(request);

        // 2. hasNext 판단 로직
        boolean hasNext = false;
        if (result.size() > request.getSize()) {
            hasNext = true;
            result.remove(result.size() - 1); // 확인용으로 가져온 마지막 1개 제거
        }

        // 3. 결과 반환
        return new com.practice.OAuth2.domain.attraction.dto.AttractionSliceResponse(
                result, hasNext, request.getPage());
    }

    @Transactional(readOnly = true)
    public AttractionResponse getAttractionDetail(Integer attractionId) {
        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행지를 찾을 수 없습니다. ID: " + attractionId
                ));

        return new AttractionResponse(attraction);
    }


    //  MyBatis 테스트용 메서드
    public List<AttractionResponse> getAttractionListTest() {
        return attractionMapper.findAllAttractions();
    }

    @Transactional
    public void toggleLike(Long userId, Integer attractionId) {
        com.practice.OAuth2.domain.user.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attraction not found"));

        placeLikeRepository.findByUserAndAttraction(user, attraction)
                .ifPresentOrElse(
                        placeLike -> {
                            placeLikeRepository.delete(placeLike);
                            attractionRepository.decrementLikeCount(attractionId);
                        },
                        () -> {
                            com.practice.OAuth2.domain.attraction.entity.PlaceLike newLike = new com.practice.OAuth2.domain.attraction.entity.PlaceLike(user, attraction);
                            placeLikeRepository.save(newLike);
                            attractionRepository.incrementLikeCount(attractionId);
                        }
                );
    }

    @Transactional(readOnly = true)
    public List<AttractionResponse> getMyLikedAttractions(Long userId) {
        com.practice.OAuth2.domain.user.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return placeLikeRepository.findAllByUser(user).stream()
                .map(placeLike -> new AttractionResponse(placeLike.getAttraction()))
                .collect(Collectors.toList());
    }
}
