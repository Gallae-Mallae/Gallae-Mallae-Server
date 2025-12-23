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
    // 1. [Sync] MySQL -> Pinecone
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
                        log.info(">> [Skip] 제외된 데이터: {}", title);
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

    // =================================================================================
    // 2. [Chat] RAG 채팅 (키워드 필터 + Top-K 셔플 + 정직한 응답)
    // =================================================================================
    public AiResponse chat(String message) {
        // 1. 의도 분석
        SearchIntent intent = analyzeUserIntent(message);
        log.info(">>> [Chat] 분석 - Region: {}, Query: {}", intent.region, intent.query);

        List<Double> vector = getOpenAiEmbedding(intent.query);

        // 2. [1차 검색] Pinecone (유사도 순 100개)
        List<Integer> ids = searchPinecone(vector, 100, intent.region);

        if (ids.isEmpty()) {
            log.info(">>> [Chat] 필터 결과 없음. 전국 재검색.");
            ids = searchPinecone(vector, 50, null);
        }

        if (ids.isEmpty()) return new AiResponse("조건에 맞는 장소를 찾지 못했습니다.", new ArrayList<>());

        // 3. DB 조회
        List<AttractionResponse2> candidates = attractionMapper.findAllByIds(ids);

        // 4. [주소 필터링]
        List<AttractionResponse2> addressFiltered = filterByAddressKeyword(candidates, message);
        if (addressFiltered.isEmpty()) {
            log.warn("!!! [Chat] 주소 매칭 실패. 원본 후보군 사용.");
            addressFiltered = candidates;
        }

        // =========================================================================
        // 🔥 [Step 1] Pinecone 랭킹(정확도) 순서 복구
        // =========================================================================
        List<AttractionResponse2> sortedPool = new ArrayList<>();
        Map<Integer, AttractionResponse2> candidateMap = addressFiltered.stream()
                .collect(Collectors.toMap(AttractionResponse2::getAttractionId, item -> item, (a, b) -> a));

        for (Integer id : ids) {
            if (candidateMap.containsKey(id)) {
                sortedPool.add(candidateMap.get(id));
            }
        }

        // =========================================================================
        // 🔥 [Step 2] 강력한 키워드 필터 ("사우나" 없으면 삭제) -> 없으면 바로 종료!
        // =========================================================================
        List<AttractionResponse2> keywordFiltered = filterByRelevance(sortedPool, intent.query);

        if (keywordFiltered.isEmpty()) {
            // 여기가 핵심입니다. 없으면 억지로 유사도 결과(sortedPool)를 쓰지 않고 끝냅니다.
            log.info(">>> [Chat] '{}' 키워드 포함 장소 0개. 빈 결과 반환.", intent.query);
            return new AiResponse("죄송합니다. 요청하신 '" + intent.query + "' 관련 장소 정보를 찾을 수 없습니다.", new ArrayList<>());
        }

        // =========================================================================
        // 🔥 [Step 3] Top-K 셔플 (상위 10개만 뽑아서 섞음 -> 다양성 확보)
        // =========================================================================
        List<AttractionResponse2> topTier = keywordFiltered.stream()
                .limit(10) // 1티어 10개 추출
                .collect(Collectors.toList());

        if (!topTier.isEmpty()) {
            Collections.shuffle(topTier); // 1티어 내에서 순서 섞기
        }

        // 최종 3개 선택 (데이터가 1개면 1개만 선택됨)
        List<AttractionResponse2> selected = topTier.stream()
                .limit(3)
                .collect(Collectors.toList());

        String context = selected.stream()
                .map(p -> String.format("(ID:%d) %s - %s : %s",
                        p.getAttractionId(), p.getTitle(), p.getAddress(),
                        (p.getOverview() != null ? p.getOverview().substring(0, Math.min(p.getOverview().length(), 100)) : "")))
                .collect(Collectors.joining("\n\n"));

        // =========================================================================
        // 🔥 [프롬프트 수정] "3개 강요" 삭제 -> "있는 만큼만 추천해"
        // =========================================================================
        String sysMsg = "당신은 정직한 한국 여행 가이드입니다. 아래 [추천 후보 목록]을 기반으로 답변하세요.\n" +
                "1. **후보 목록에 있는 장소만 추천하세요.** (목록이 1개면 1개만, 3개면 3개만 추천)\n" +
                "2. **절대 없는 장소를 지어내거나, 목록에 없는 장소를 추가하지 마세요.**\n" +
                "3. 각 장소마다 추천하는 이유를 매력적으로 설명하세요.\n" +
                "4. 답변 마지막 줄에 [IDS: 1, 2, 3] 형태로 추천한 장소의 ID만 나열하세요.";

        String userMsg = String.format("[추천 후보 목록]\n%s\n\n질문: %s", context, message);
        String rawAnswer = callChatGpt(sysMsg, userMsg, 0.5);

        return parseResponse(rawAnswer, selected);
    }

    // =================================================================================
    // 3. Helper Methods
    // =================================================================================

    // 🔥 [신규 메서드] 키워드 포함 여부 검사 (제목, 설명에 검색어가 있나?)
    private List<AttractionResponse2> filterByRelevance(List<AttractionResponse2> list, String query) {
        if (query == null || query.trim().isEmpty()) return list;

        String[] keywords = query.split("\\s+");
        List<String> validKeywords = new ArrayList<>();
        for (String k : keywords) {
            // "추천", "알려줘" 같은 의미 없는 단어 제외
            if (k.length() >= 2 && !k.equals("추천") && !k.equals("알려줘")) {
                validKeywords.add(k);
            }
        }

        if (validKeywords.isEmpty()) return list;

        List<AttractionResponse2> result = new ArrayList<>();
        for (AttractionResponse2 item : list) {
            boolean isMatch = false;
            String totalText = (item.getTitle() + " " + item.getContentTypeName() + " " + item.getOverview());

            // 검색어가 하나라도 포함되면 통과 (제목이나 설명에 '사우나'가 있어야 함)
            for (String key : validKeywords) {
                if (totalText.contains(key)) {
                    isMatch = true;
                    break;
                }
            }
            if (isMatch) result.add(item);
        }

        log.info(">>> [키워드 필터] 입력 {}개 -> 출력 {}개 (키워드: {})", list.size(), result.size(), validKeywords);
        return result;
    }

    private static class SearchIntent {
        String region;
        String query;
        public SearchIntent(String region, String query) {
            this.region = region;
            this.query = query;
        }
    }

    private String parseRegionStandard(String address) {
        if (address == null || address.trim().isEmpty()) return "기타";
        String trimmed = address.trim();
        return (trimmed.length() >= 2) ? trimmed.substring(0, 2) : trimmed;
    }

    private SearchIntent analyzeUserIntent(String message) {
        String prompt = "당신은 한국 지리 전문가입니다.\n" +
                "사용자의 질문을 분석하여 다음 두 가지를 추출하세요.\n\n" +
                "1. **지역(Region)**: 질문에 포함된 지명(동, 구, 랜드마크 등)이 속한 **'광역자치단체'의 앞 두 글자**를 추출하세요.\n" +
                "   **[필수] 당신의 지리적 지식을 활용하여, 예시에 없더라도 정확한 광역단체를 찾아내세요.**\n" +
                "   [매핑 예시]\n" +
                "   - 서울, 강남, 홍대 -> '서울'\n" +
                "   - 경기, 가평, 판교 -> '경기'\n" +
                "   - 인천, 송도 -> '인천'\n" +
                "   - 강원, 춘천, 속초 -> '강원'\n" +
                "   - 부산, 해운대, 서면 -> '부산'\n" +
                "   - 대구, 동성로 -> '대구', 대전 -> '대전', 광주 -> '광주', 울산 -> '울산', 세종 -> '세종'\n" +
                "   - 제주, 애월 -> '제주'\n" +
                "   - 충청(천안/청주 등) -> '충청', 전라(전주/여수 등) -> '전라', 경상(경주/포항 등) -> '경상'\n" +
                "   - 지역 언급 없음 -> 'NONE'\n" +
                "2. **검색어(Query)**: 세부 지명을 **포함한** 자연어 검색 문장\n\n" +
                "입력: " + message + "\n" +
                "출력: Region: [2글자지역명] | Query: [검색어]";

        String result = callChatGpt("검색 분석기", prompt, 0.0);
        try {
            String[] parts = result.split("\\|");
            String regionPart = parts[0].replace("Region:", "").trim();
            String queryPart = parts.length > 1 ? parts[1].replace("Query:", "").trim() : message;
            if ("NONE".equalsIgnoreCase(regionPart)) regionPart = null;
            return new SearchIntent(regionPart, queryPart);
        } catch (Exception e) {
            return new SearchIntent(null, message);
        }
    }

    private List<AttractionResponse2> filterByAddressKeyword(List<AttractionResponse2> candidates, String userMessage) {
        String targetLocation = null;
        for (AttractionResponse2 attr : candidates) {
            if (attr.getAddress() == null) continue;
            String[] tokens = attr.getAddress().split(" ");
            for (String token : tokens) {
                if (token.length() >= 2 && (token.endsWith("구") || token.endsWith("군") || token.endsWith("시"))) {
                    String core = token.substring(0, token.length() - 1);
                    if (userMessage.contains(core)) {
                        targetLocation = core;
                        break;
                    }
                }
            }
            if (targetLocation != null) break;
        }

        if (targetLocation == null) return candidates;

        log.info(">>> [2차 필터] '{}' 포함 주소만 필터링.", targetLocation);
        List<AttractionResponse2> filtered = new ArrayList<>();
        for (AttractionResponse2 attr : candidates) {
            if (attr.getAddress() != null && attr.getAddress().contains(targetLocation)) {
                filtered.add(attr);
            }
        }
        return filtered;
    }

    private List<Integer> searchPinecone(List<Double> vector, int topK, String filterRegion) {
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
                        Map<String, Object> metadata = (Map<String, Object>) m.get("metadata");
                        return Integer.parseInt(metadata.get("attractionId").toString());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Pinecone 오류: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // =================================================================================
    // 4. API Utils
    // =================================================================================

    private AiResponse parseResponse(String raw, List<AttractionResponse2> candidates) {
        Set<Integer> selectedIds = new HashSet<>();
        String aiMessage = raw;
        if (raw.contains("[IDS:")) {
            int idx = raw.lastIndexOf("[IDS:");
            aiMessage = raw.substring(0, idx).trim();
            try {
                String idPart = raw.substring(idx + 5).replace("]", "").trim();
                for (String s : idPart.split(",")) { if(!s.trim().isEmpty()) selectedIds.add(Integer.parseInt(s.trim())); }
            } catch (Exception e) {}
        }
        Pattern idPattern = Pattern.compile("\\(ID:(\\d+)\\)");
        Matcher matcher = idPattern.matcher(aiMessage);
        while (matcher.find()) selectedIds.add(Integer.parseInt(matcher.group(1)));
        aiMessage = matcher.replaceAll("").replaceAll("\\s{2,}", " ").trim();

        List<AiResponse.PlaceInfo> places = candidates.stream()
                .filter(c -> selectedIds.contains(c.getAttractionId()))
                .map(c -> new AiResponse.PlaceInfo(c.getAttractionId(), c.getTitle(), c.getAddress(), c.getImageUrl()))
                .collect(Collectors.toList());
        return new AiResponse(aiMessage, places);
    }

    private String callChatGpt(String role, String content, double temp) {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(Map.of("role", "system", "content", role), Map.of("role", "user", "content", content)),
                "temperature", temp
        );
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders(openAiKey)), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) ((Map) response.getBody()).get("choices");
            return (String) ((Map) choices.get(0).get("message")).get("content");
        } catch (Exception e) { return "오류 발생"; }
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