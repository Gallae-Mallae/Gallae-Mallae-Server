package com.practice.OAuth2.domain.ai.service;

import com.practice.OAuth2.domain.ai.dto.AiResponse;
import com.practice.OAuth2.domain.attraction.dto.AttractionResponse2;
import com.practice.OAuth2.domain.attraction.mapper.AttractionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AttractionMapper attractionMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key}")
    private String openAiKey;
    @Value("${ai.pinecone.api-key}")
    private String pineconeKey;
    @Value("${ai.pinecone.host}")
    private String pineconeHost;

    // =================================================================================
    // 1. [Sync] MySQL -> Pinecone (사용자 원본 로직 + 실내외 + 키워드 확장)
    // =================================================================================
    public void syncMysqlToPinecone(int startPage) {
        int page = startPage;
        int batchSize = 10;
        log.info("🚀 [Sync] 샌드위치 & 키워드 그물망 임베딩 시작 (Page: {})", page);

        while (true) {
            int offset = page * batchSize;
            List<AttractionResponse2> attractions = attractionMapper.findRagData(batchSize, offset);

            if (attractions == null || attractions.isEmpty()) break;

            try {
                List<String> embedTexts = new ArrayList<>();
                List<String> idList = new ArrayList<>();
                List<Map<String, Object>> metadataList = new ArrayList<>();

                for (AttractionResponse2 attr : attractions) {
                    if (shouldSkipData(attr.getTitle())) continue;

                    // 시도/구군 명칭 조합 (샌드위치 재료)
                    String sido = (attr.getSidoName() != null) ? attr.getSidoName() : "";
                    String gugun = (attr.getGugunName() != null) ? attr.getGugunName() : "";
                    String locationTag = (sido + " " + gugun).trim();
                    if (locationTag.isEmpty()) locationTag = "지역 미상";

                    String category = (attr.getContentTypeName() != null) ? attr.getContentTypeName() : "관광지";

                    // [수정된 통합 프롬프트] 원본 로직 유지 + 실내외 + 키워드 그물망
                    String prompt = String.format(
                            "데이터 정보 - [명칭: %s, 위치: %s, 유형: %s, 설명: %s]\n\n" +
                                    "당신은 **통찰력 있는 여행지 분석가이자 검색 엔진 최적화 전문가**입니다.\n" +
                                    "주어진 데이터를 바탕으로 아래 지침에 따라 장소를 분석하세요.\n\n" +
                                    "## 1. 공간 및 키워드 분석 (필수)\n" +
                                    "- **공간 구분**: 이 장소가 **실내(Indoor)**인지 **실외(Outdoor)**인지 반드시 판별하세요.\n" +
                                    "- **키워드 그물망(Keyword Web)**: 장소의 성격을 나타내는 단어와 그 **동의어, 연관어, 유사어**를 최대한 많이 추출하세요.\n" +
                                    "  (예: '스파' -> 목욕탕, 사우나, 온천, 세신, 힐링, 물놀이 / '아이' -> 어린이, 키즈, 가족, 체험)\n\n" +
                                    "## 2. 장소 분류 기준 (우선순위 순)\n" +
                                    "1. **단순 유적 (Strict)**: 명칭에 **'터', '비석', '묘', '생가', '탑', '비'**가 포함됨 -> 엄격하게 '역사 탐방', '잠시 경유'로 제한.\n" +
                                    "2. **산책 코스 (Walking)**: 명칭에 **'거리', '길', '공원', '마을', '시장', '광장', '숲', '둘레길'** 포함 -> '산책', '데이트', '나들이'로 긍정 묘사.\n" +
                                    "3. **주요 명소 (Major)**: 테마파크, 놀이공원, 아쿠아리움, 랜드마크 타워 -> '가족, 연인, 관광객 필수 코스'.\n" +
                                    "4. **일반 방문지 (General)**: 위 3가지 키워드에 해당하지 않는 모든 곳.\n\n" +
                                    "## 3. 출력 형식\n" +
                                    "이곳은 **[실내/실외] [장소 성격]**입니다. **[분위기]** 분위기이며, **[타겟]**이 **[활동]**을 하기에 좋습니다.\n" +
                                    "관련 키워드: [핵심 단어들과 모든 유사어/동의어를 쉼표로 나열]",
                            attr.getTitle(), locationTag, category, attr.getOverview()
                    );

                    String enrichedText = callChatGpt("검색 최적화 분석가", prompt, 0.4);

                    // [샌드위치 전략] 지명 앞뒤 배치 + 풍성해진 키워드 문장
                    String finalSearchText = String.format("[지역: %s] 명칭: %s | 유형: %s | %s [위치: %s]",
                            locationTag, attr.getTitle(), category, enrichedText, locationTag);

                    embedTexts.add(finalSearchText);
                    idList.add(String.valueOf(attr.getAttractionId()));

                    metadataList.add(Map.of(
                            "attractionId", attr.getAttractionId(),
                            "title", attr.getTitle(),
                            "address", attr.getAddress() != null ? attr.getAddress() : "",
                            "category", category,
                            "region", parseRegionStandard(attr.getAddress()),
                            "text", finalSearchText
                    ));
                }

                if (!embedTexts.isEmpty()) {
                    List<List<Double>> vectors = getOpenAiEmbeddingBatch(embedTexts);
                    upsertToPineconeBatch(idList, vectors, metadataList);
                }
                log.info(">> [Sync] {} 페이지 가공 완료.", page);
                Thread.sleep(800);
            } catch (Exception e) { break; }
            page++;
        }
    }

    // =================================================================================
    // 2. [Chat] RAG 채팅 (기존 로직 100% 유지)
    // =================================================================================
    public AiResponse chat(String message) {
        SearchIntent intent = analyzeUserIntent(message);
        List<Double> vector = getOpenAiEmbedding(intent.query);
        List<ScoredItem> scoredItems = searchPineconeWithScore(vector, 10, intent.region);

        if (scoredItems.isEmpty()) return createMapSearchResponse(intent.query);

        List<Integer> candidateIds = scoredItems.stream().map(i -> i.id).collect(Collectors.toList());
        List<AttractionResponse2> candidates = attractionMapper.findAllByIds(candidateIds);

        List<Integer> verifiedIds = verifyCandidatesWithGpt(message, candidates);
        if (verifiedIds.isEmpty()) return createMapSearchResponse(message);

        List<AttractionResponse2> finalPlaces = candidates.stream()
                .filter(c -> verifiedIds.contains(c.getAttractionId()))
                .limit(3).collect(Collectors.toList());

        return generateFinalRecommendation(message, finalPlaces);
    }

    private List<Integer> verifyCandidatesWithGpt(String userMessage, List<AttractionResponse2> candidates) {
        if (candidates.isEmpty()) return Collections.emptyList();
        StringBuilder sb = new StringBuilder();
        for (AttractionResponse2 attr : candidates) {
            sb.append(String.format("- [ID:%d] %s (%s)\n", attr.getAttractionId(), attr.getTitle(), attr.getAddress()));
        }
        String result = callChatGpt("검증관", "질문: " + userMessage + "\n후보:\n" + sb.toString() + "\n맞는 ID만 [1,2] 형식으로 답해.", 0.0);
        try {
            Pattern p = Pattern.compile("\\[.*?\\]");
            Matcher m = p.matcher(result);
            if (m.find()) {
                String clean = m.group().replace("[", "").replace("]", "");
                if (clean.isEmpty()) return Collections.emptyList();
                return Arrays.stream(clean.split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
            }
        } catch (Exception e) { log.error("Verify Error"); }
        return Collections.emptyList();
    }

    private AiResponse createMapSearchResponse(String query) {
        String url = "https://map.naver.com/p/search/" + query.replace(" ", "%20");
        return new AiResponse("찾으시는 장소가 없네요. 지도를 확인해보세요.\n\n[지도 이동](" + url + ")", new ArrayList<>());
    }

    private AiResponse generateFinalRecommendation(String message, List<AttractionResponse2> selected) {
        String context = selected.stream().map(p -> "- " + p.getTitle() + ": " + p.getAddress()).collect(Collectors.joining("\n"));
        String answer = callChatGpt("가이드", "질문: " + message + "\n장소:\n" + context, 0.7);
        List<AiResponse.PlaceInfo> places = selected.stream()
                .map(c -> new AiResponse.PlaceInfo(c.getAttractionId(), c.getTitle(), c.getAddress(), c.getImageUrl()))
                .collect(Collectors.toList());
        return new AiResponse(answer, places);
    }

    private SearchIntent analyzeUserIntent(String message) {
        String prompt = "지역(Region)과 상세검색어(Query)를 추출하세요. Region 없으면 NONE.\n" +
                "Query는 '[지역: OO] 검색어 [위치: OO]' 포맷을 참고하세요.\n" +
                "출력: Region: [지역] | Query: [검색어]";
        String result = callChatGpt("의도 분석기", prompt + "\n입력: " + message, 0.0);
        try {
            String[] parts = result.split("\\|");
            String r = parts[0].replace("Region:", "").trim();
            String q = parts[1].replace("Query:", "").trim();
            return new SearchIntent(r.equalsIgnoreCase("NONE") ? null : r, q);
        } catch (Exception e) { return new SearchIntent(null, message); }
    }

    // =================================================================================
    // 3. [Type Safe] GPT 호출 및 유틸리티
    // =================================================================================
    private String callChatGpt(String role, String content, double temp) {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(Map.of("role", "system", "content", role), Map.of("role", "user", "content", content)));
        body.put("temperature", temp);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, createHeaders(openAiKey)),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> respBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) { return ""; }
    }

    private List<ScoredItem> searchPineconeWithScore(List<Double> vector, int topK, String filterRegion) {
        String url = pineconeHost + "/query";
        Map<String, Object> body = new HashMap<>();
        body.put("vector", vector); body.put("topK", topK); body.put("includeMetadata", true);
        if (filterRegion != null) body.put("filter", Map.of("region", Map.of("$eq", filterRegion)));
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, createPineconeHeaders()),
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.getBody().get("matches");
            return matches.stream().map(m -> {
                Map<String, Object> metadata = (Map<String, Object>) m.get("metadata");
                return new ScoredItem(Integer.parseInt(metadata.get("attractionId").toString()), Double.parseDouble(m.get("score").toString()));
            }).collect(Collectors.toList());
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private List<List<Double>> getOpenAiEmbeddingBatch(List<String> texts) {
        String url = "https://api.openai.com/v1/embeddings";
        Map<String, Object> body = Map.of("model", "text-embedding-3-small", "input", texts);
        try {
            Map<String, Object> resp = restTemplate.postForObject(url, new HttpEntity<>(body, createHeaders(openAiKey)), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
            return data.stream().map(d -> (List<Double>) d.get("embedding")).collect(Collectors.toList());
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private List<Double> getOpenAiEmbedding(String text) { return getOpenAiEmbeddingBatch(List.of(text)).get(0); }

    private void upsertToPineconeBatch(List<String> ids, List<List<Double>> vectors, List<Map<String, Object>> metas) {
        String url = pineconeHost + "/vectors/upsert";
        List<Map<String, Object>> v = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) v.add(Map.of("id", ids.get(i), "values", vectors.get(i), "metadata", metas.get(i)));
        restTemplate.postForEntity(url, new HttpEntity<>(Map.of("vectors", v), createPineconeHeaders()), String.class);
    }

    private HttpHeaders createHeaders(String k) { HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setBearerAuth(k); return h; }
    private HttpHeaders createPineconeHeaders() { HttpHeaders h = new HttpHeaders(); h.set("Api-Key", pineconeKey); h.setContentType(MediaType.APPLICATION_JSON); return h; }

    private boolean shouldSkipData(String title) {
        if (title == null) return true;
        String t = title.replace(" ", "");
        return t.contains("유치원") || t.contains("어린이집") || t.contains("경로당") || t.contains("주민센터");
    }

    private String parseRegionStandard(String address) {
        if (address == null || address.trim().isEmpty()) return "기타";
        String trimmed = address.trim();
        return (trimmed.length() >= 2) ? trimmed.substring(0, 2) : trimmed;
    }

    private static class SearchIntent { String region; String query; SearchIntent(String r, String q) { this.region = r; this.query = q; } }
    private static class ScoredItem { int id; double score; ScoredItem(int i, double s) { this.id = i; this.score = s; } }
}