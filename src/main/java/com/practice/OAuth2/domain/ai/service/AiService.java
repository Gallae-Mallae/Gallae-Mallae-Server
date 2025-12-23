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
    // 1. [Sync] MySQL -> Pinecone (사용자님 원본 코드 100% 유지)
    // =================================================================================
    public void syncMysqlToPinecone(int startPage) {
        int page = startPage;
        int batchSize = 10;
        log.info("=========== [Sync] 데이터 정제 및 임베딩 시작 (Page: {}) ===========", page);

        while (true) {
            int offset = page * batchSize;
            List<AttractionResponse2> attractions = attractionMapper.findRagData(batchSize, offset);

            if (attractions.isEmpty()) {
                log.info(">> [Sync] 데이터 처리 종료.");
                break;
            }

            try {
                List<String> embedTexts = new ArrayList<>();
                List<String> idList = new ArrayList<>();
                List<Map<String, Object>> metadataList = new ArrayList<>();

                for (AttractionResponse2 attr : attractions) {
                    String title = attr.getTitle();
                    if (shouldSkipData(title)) {
                        continue;
                    }

                    String addr = (attr.getAddress() != null) ? attr.getAddress() : "위치 정보 없음";
                    String category = (attr.getContentTypeName() != null) ? attr.getContentTypeName() : "관광지";
                    String overview = (attr.getOverview() != null) ? attr.getOverview() : "";
                    String standardRegion = parseRegionStandard(addr);

                    String prompt = String.format(
                            "데이터 정보 - [명칭: %s, 위치: %s, 유형: %s, 설명: %s]\n\n" +
                                    "당신은 **통찰력 있는 여행지 분석가**입니다.\n" +
                                    "주어진 데이터를 바탕으로 장소의 성격을 분석하되, **장소의 형태(Type)**에 따라 유연하게 판단하세요.\n" +
                                    "**[핵심 규칙]**: 억지 분류 금지. 아래 기준에 딱 맞지 않으면 4번(일반 방문지)으로 분류하여 설명 내용을 따르세요.\n\n" +
                                    "## 1. 장소 분류 기준 (우선순위 순)\n" +
                                    "1. **단순 유적 (Strict)**: 명칭에 **'터', '비석', '묘', '생가', '탑', '비'**가 포함됨 -> 엄격하게 '역사 탐방', '잠시 경유'로 제한.\n" +
                                    "2. **산책 코스 (Walking)**: 명칭에 **'거리', '길', '공원', '마을', '시장', '광장', '숲', '둘레길'** 포함 -> '산책', '데이트', '나들이'로 긍정 묘사.\n" +
                                    "3. **주요 명소 (Major)**: 테마파크, 놀이공원, 아쿠아리움, 랜드마크 타워 -> '가족, 연인, 관광객 필수 코스'.\n" +
                                    "4. **일반 방문지 (General)**: **위 3가지 키워드에 해당하지 않는 모든 곳** (예: 미술관, 도서관, 박물관, 해수욕장, 산, 체험관, 캠핑장 등).\n" +
                                    "   -> **설명(Overview)을 보고 판단**: 전시 위주면 '문화 관람', 자연 위주면 '휴식/힐링', 체험 위주면 '이색 체험'으로 자연스럽게 서술.\n\n" +
                                    "## 2. 분석 가이드\n" +
                                    "- **분위기**: 장소 특성에 맞게 (예: 도서관='조용한', 시장='활기찬', 해수욕장='시원한')\n" +
                                    "- **타겟**: (예: 미술관='문화인', 캠핑장='캠핑족/가족')\n\n" +
                                    "## 3. 출력 형식 (자연스러운 한국어)\n" +
                                    "이곳은 **[장소 성격(분류 결과)]**입니다. **[분위기]** 분위기이며, **[타겟]**이 **[활동]**을 하기에 좋습니다. 태그: #키워드1 #키워드2 #[분류명]",
                            title, addr, category, overview
                    );

                    String enrichedText = callChatGpt("여행지 분석가", prompt, 0.4);
                    String finalSearchText = String.format("명칭: %s. 위치: %s. %s", title, addr, enrichedText);

                    embedTexts.add(finalSearchText);
                    idList.add(String.valueOf(attr.getAttractionId()));

                    metadataList.add(Map.of(
                            "attractionId", attr.getAttractionId(),
                            "title", title,
                            "address", addr,
                            "region", standardRegion,
                            "text", finalSearchText
                    ));
                }

                if (!embedTexts.isEmpty()) {
                    List<List<Double>> vectors = getOpenAiEmbeddingBatch(embedTexts);
                    upsertToPineconeBatch(idList, vectors, metadataList);
                }

                log.info(">> [Sync] {} 페이지 처리 완료.", page);
                Thread.sleep(800);

            } catch (Exception e) {
                log.error("!! [Sync] 페이지 {} 에러: {}", page, e.getMessage());
                break;
            }
            page++;
        }
    }

    private boolean shouldSkipData(String title) {
        if (title == null) return true;
        String t = title.replace(" ", "");
        return t.contains("유치원") || t.contains("어린이집") || t.contains("경로당") ||
                t.contains("주민센터") || t.contains("보건소") || t.contains("행정복지센터");
    }

    private String parseRegionStandard(String address) {
        if (address == null || address.trim().isEmpty()) return "기타";
        String trimmed = address.trim();
        return (trimmed.length() >= 2) ? trimmed.substring(0, 2) : trimmed;
    }

    // =================================================================================
    // 2. [Chat] RAG 채팅 (NEW: 검색 -> LLM 심판 -> 지도 분기)
    // =================================================================================
    public AiResponse chat(String message) {
        // 1. 의도 분석
        SearchIntent intent = analyzeUserIntent(message);
        log.info(">>> [Chat] 분석 결과 - Region: [{}], Query: [{}]", intent.region, intent.query);

        // 2. 임베딩 및 Pinecone 검색 (일단 5개 후보 추출)
        List<Double> vector = getOpenAiEmbedding(intent.query);
        List<ScoredItem> scoredItems = searchPineconeWithScore(vector, 5, intent.region);

        if (scoredItems.isEmpty()) {
            log.info(">>> [Chat] 1차 검색 실패 -> 지도 검색 유도");
            return createMapSearchResponse(intent.query);
        }

        // 3. DB에서 후보군 상세 정보 조회
        List<Integer> candidateIds = scoredItems.stream().map(i -> i.id).collect(Collectors.toList());
        List<AttractionResponse2> candidates = attractionMapper.findAllByIds(candidateIds);

        // 4. [핵심] LLM 심판 (Judge): 과연 이 장소들이 질문과 맞는가?
        // 여기서 "부산" 물어봤는데 "송파" 나오면 다 걸러짐.
        List<Integer> verifiedIds = verifyCandidatesWithGpt(message, candidates);

        // 5. 결과 분기 처리
        if (verifiedIds.isEmpty()) {
            // 검증 결과 쓸만한 게 하나도 없다 -> 지도 검색으로 짬때리기
            log.info(">>> [Chat] LLM 검증 결과 적합한 장소 없음 -> 지도 검색 유도");
            return createMapSearchResponse(message);
        }

        // 6. 검증 통과한 장소로 최종 답변 생성
        List<AttractionResponse2> finalPlaces = candidates.stream()
                .filter(c -> verifiedIds.contains(c.getAttractionId()))
                .limit(3) // 최대 3개
                .collect(Collectors.toList());

        return generateFinalRecommendation(message, finalPlaces);
    }

    // =================================================================================
    // [Chat Helpers] 검증, 지도생성, 최종답변 (새로 추가됨)
    // =================================================================================

    // LLM 심판 로직
    private List<Integer> verifyCandidatesWithGpt(String userMessage, List<AttractionResponse2> candidates) {
        if (candidates.isEmpty()) return Collections.emptyList();

        StringBuilder candidatesText = new StringBuilder();
        for (AttractionResponse2 attr : candidates) {
            candidatesText.append(String.format("- [ID:%d] 명칭: %s, 주소: %s, 설명: %s\n",
                    attr.getAttractionId(), attr.getTitle(), attr.getAddress(),
                    (attr.getOverview() != null && attr.getOverview().length() > 50) ? attr.getOverview().substring(0, 50) : ""));
        }

        String systemPrompt = "당신은 엄격한 '검색 결과 검증관'입니다.\n" +
                "사용자의 질문과 검색된 장소 후보들을 비교하여, 질문의 의도(특히 '지역'과 '장소 유형')에 정확히 부합하는 장소만 골라내세요.\n" +
                "1. 사용자가 특정 지역(예: 부산 동래구)을 원했는데 다른 지역(예: 서울 송파구)이 나오면 과감히 탈락시키세요.\n" +
                "2. 적합한 장소가 있다면 그 장소의 ID만 JSON 리스트로 반환하세요. (예: [123, 456])\n" +
                "3. 적합한 장소가 하나도 없다면 빈 리스트 '[]'를 반환하세요.\n" +
                "4. 오직 숫자 리스트만 반환하세요.";

        String userPrompt = String.format("사용자 질문: \"%s\"\n\n검색 후보군:\n%s", userMessage, candidatesText.toString());

        String result = callChatGpt("검색 검증관", systemPrompt + "\n" + userPrompt, 0.0);

        try {
            Pattern pattern = Pattern.compile("\\[.*?\\]");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                String jsonContent = matcher.group();
                if (jsonContent.equals("[]")) return Collections.emptyList();
                String cleanContent = jsonContent.replace("[", "").replace("]", "");
                return Arrays.stream(cleanContent.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("검증 결과 파싱 실패: {}", result);
        }
        return Collections.emptyList();
    }

    // 지도 검색 유도 응답
    private AiResponse createMapSearchResponse(String query) {
        String searchUrl = "https://map.naver.com/p/search/" + query.replace(" ", "%20");
        String message = String.format("죄송합니다. 제가 가진 데이터에는 조건에 딱 맞는 장소가 없네요.\n" +
                "대신 실시간 지도로 확인해보시겠어요? 아래 링크를 클릭해주세요.\n\n" +
                "[지도에서 '%s' 검색하기](%s)", query, searchUrl);

        return new AiResponse(message, new ArrayList<>());
    }

    // 최종 추천 멘트 생성
    private AiResponse generateFinalRecommendation(String message, List<AttractionResponse2> selected) {
        String context = selected.stream()
                .map(p -> String.format("- %s (위치: %s): %s",
                        p.getTitle(), p.getAddress(),
                        (p.getOverview() != null ? p.getOverview().substring(0, Math.min(p.getOverview().length(), 100)) : "")))
                .collect(Collectors.joining("\n\n"));

        String sysMsg = "당신은 여행 가이드입니다. 아래 선별된 장소들에 대해 사용자에게 매력적으로 추천해주세요. 없는 내용은 지어내지 마세요.";
        String userMsg = String.format("질문: %s\n\n선별된 장소 정보:\n%s", message, context);

        String answer = callChatGpt(sysMsg, userMsg, 0.7);

        List<AiResponse.PlaceInfo> places = selected.stream()
                .map(c -> new AiResponse.PlaceInfo(
                        c.getAttractionId(),
                        c.getTitle(),
                        c.getAddress(),
                        c.getImageUrl()
                ))
                .collect(Collectors.toList());

        return new AiResponse(answer, places);
    }

    // =================================================================================
    // [Common Helpers] 의도 분석, Pinecone, GPT, Embedding (공통 사용)
    // =================================================================================

    // 의도 분석기 (지역 필터링을 위해 유지)
    private SearchIntent analyzeUserIntent(String message) {
        String prompt = "사용자의 입력에서 'Region(광역자치단체 표준명)'과 'Query(상세 검색어)'를 추출하세요.\n" +
                "- Region 예: '송파구'->'서울', '해운대'->'부산'. 없으면 'NONE'.\n" +
                "- Query: 광역명 제외, 구체적 지명 및 목적 유지.\n" +
                "출력형식: Region: [지역] | Query: [검색어]";

        String result = callChatGpt("검색 의도 분석기", prompt + "\n입력: " + message, 0.0);

        try {
            String[] parts = result.split("\\|");
            String regionPart = parts[0].replace("Region:", "").trim();
            if ("NONE".equalsIgnoreCase(regionPart)) regionPart = null;
            String queryPart = parts.length > 1 ? parts[1].replace("Query:", "").trim() : message;
            return new SearchIntent(regionPart, queryPart);
        } catch (Exception e) {
            return new SearchIntent(null, message);
        }
    }

    private static class SearchIntent { String region; String query; public SearchIntent(String region, String query) { this.region = region; this.query = query; } }

    private static class ScoredItem {
        int id; double score;
        public ScoredItem(int id, double score) { this.id = id; this.score = score; }
    }

    private List<ScoredItem> searchPineconeWithScore(List<Double> vector, int topK, String filterRegion) {
        String url = pineconeHost + "/query";
        Map<String, Object> body = new HashMap<>();
        body.put("vector", vector);
        body.put("topK", topK);
        body.put("includeMetadata", true);

        if (filterRegion != null && !filterRegion.isEmpty()) {
            body.put("filter", Map.of("region", Map.of("$eq", filterRegion)));
        }

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, createPineconeHeaders()), Map.class);
            List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.getBody().get("matches");
            return matches.stream()
                    .map(m -> {
                        int id = Integer.parseInt(((Map) m.get("metadata")).get("attractionId").toString());
                        double score = Double.parseDouble(m.get("score").toString());
                        return new ScoredItem(id, score);
                    })
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Pinecone 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String callChatGpt(String role, String content, double temp) {
        // user content 부분만 role 없이 string으로 들어오는 경우와, system prompt가 분리된 경우 처리
        // 위에서 호출할 때 callChatGpt(sysMsg, userMsg, temp) 형태로 호출하는 로직이 있는데
        // 현재 메서드 시그니처는 callChatGpt(String role, String content, double temp) 임.
        // 이를 맞추기 위해 내부에서 role을 system message로 간주하고 처리.

        String url = "https://api.openai.com/v1/chat/completions";

        // role 변수에 System Prompt 전체가 들어오는 경우(analyzeUserIntent 등)와
        // role="여행지 분석가" 처럼 짧게 들어오는 경우 모두 대응
        List<Map<String, String>> messages = new ArrayList<>();

        // System Prompt
        messages.add(Map.of("role", "system", "content", role));

        // User Content (만약 content 안에 "사용자 질문:" 같은게 이미 포함되어 있으면 그대로 전송)
        messages.add(Map.of("role", "user", "content", content));

        Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", messages,
                "temperature", temp
        );
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders(openAiKey)), Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) return "";
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) return "";
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("GPT 호출 실패: {}", e.getMessage());
            return "오류 발생";
        }
    }

    private List<List<Double>> getOpenAiEmbeddingBatch(List<String> texts) {
        String url = "https://api.openai.com/v1/embeddings";
        Map<String, Object> body = Map.of("model", "text-embedding-3-small", "input", texts);
        Map resp = restTemplate.postForObject(url, new HttpEntity<>(body, createHeaders(openAiKey)), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
        return data.stream().map(d -> (List<Double>) d.get("embedding")).collect(Collectors.toList());
    }

    private List<Double> getOpenAiEmbedding(String text) {
        return getOpenAiEmbeddingBatch(List.of(text)).get(0);
    }

    private void upsertToPineconeBatch(List<String> ids, List<List<Double>> vectors, List<Map<String, Object>> metas) {
        String url = pineconeHost + "/vectors/upsert";
        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            payload.add(Map.of("id", ids.get(i), "values", vectors.get(i), "metadata", metas.get(i)));
        }
        restTemplate.postForEntity(url, new HttpEntity<>(Map.of("vectors", payload), createPineconeHeaders()), String.class);
    }

    private HttpHeaders createHeaders(String k) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(k);
        return h;
    }

    private HttpHeaders createPineconeHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Api-Key", pineconeKey);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}