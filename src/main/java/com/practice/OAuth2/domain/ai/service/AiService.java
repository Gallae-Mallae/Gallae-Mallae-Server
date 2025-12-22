package com.practice.OAuth2.domain.ai.service;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AttractionRepository attractionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key}")
    private String geminiKey;

    @Value("${ai.pinecone.api-key}")
    private String pineconeKey;

    @Value("${ai.pinecone.host}")
    private String pineconeHost;

    /**
     * MySQL 데이터를 한글 파싱하여 파인콘 벡터 DB로 동기화
     */
    public void syncMysqlToPinecone() {
        int page = 0;
        int size = 50;

        while (true) {
            Page<Attraction> attractionPage = attractionRepository.findRagData(PageRequest.of(page, size));
            if (attractionPage.isEmpty()) break;

            for (Attraction attr : attractionPage) {
                try {
                    // 1. [핵심] contentTypeId를 한글 유형으로 변환
                    String typeName = getContentTypeName(attr.getContentType().getContentTypeId());

                    // 2. 텍스트 구성 (유형 정보를 포함시켜 AI 인지력 상승)
                    String text = String.format("[장소유형: %s] 장소명: %s, 주소: %s, 설명: %s",
                            typeName,
                            attr.getTitle(),
                            attr.getAddr1(),
                            attr.getOverview());

                    // 3. Gemini 임베딩 생성
                    List<Double> vector = getEmbedding(text);

                    // 4. Pinecone에 업서트 (Metadata에 유형 정보 추가)
                    upsertToPinecone(attr.getAttrId().toString(), vector, text, typeName);

                } catch (Exception e) {
                    log.error("ID {} 처리 중 에러 발생: {}", attr.getAttrId(), e.getMessage());
                }
            }
            log.info("{} 페이지 ({}건) 동기화 진행 중...", page, attractionPage.getNumberOfElements());
            page++;
        }
        log.info("모든 데이터의 파인콘 동기화가 완료되었습니다.");
    }

    // contentTypeId 한글 파싱 유틸 메서드
    private String getContentTypeName(int typeId) {
        return switch (typeId) {
            case 12 -> "관광지";
            case 14 -> "문화시설";
            case 15 -> "축제공연행사";
            case 25 -> "여행코스";
            case 28 -> "레포츠";
            case 32 -> "숙박";
            case 38 -> "쇼핑";
            case 39 -> "음식점";
            default -> "일반 여행지";
        };
    }

    private List<Double> getEmbedding(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + geminiKey;

        Map<String, Object> request = Map.of(
                "model", "models/text-embedding-004",
                "content", Map.of("parts", List.of(Map.of("text", text)))
        );

        Map response = restTemplate.postForObject(url, request, Map.class);
        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Gemini 임베딩 API 응답이 유효하지 않습니다.");
        }
        Map embedding = (Map) response.get("embedding");
        return (List<Double>) embedding.get("values");
    }

    private void upsertToPinecone(String id, List<Double> vector, String text, String typeName) {
        String url = pineconeHost + "/vectors/upsert";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", pineconeKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
                "vectors", List.of(Map.of(
                        "id", id,
                        "values", vector,
                        "metadata", Map.of(
                                "text", text,
                                "attractionId", Integer.parseInt(id),
                                "placeType", typeName // 한글 타입 정보도 메타데이터에 추가
                        )
                ))
        );

        HttpEntity<Map> entity = new HttpEntity<>(request, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}