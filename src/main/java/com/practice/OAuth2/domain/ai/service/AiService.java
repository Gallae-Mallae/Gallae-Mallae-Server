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

    private final AttractionMapper attractionMapper; // JPA Repository 대신 MyBatis Mapper 사용
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key}")
    private String openAiKey;
    @Value("${ai.pinecone.api-key}")
    private String pineconeKey;g
    @Value("${ai.pinecone.host}")
    private String pineconeHost;

    // =================================================================================
    // 1. [Sync] MySQL -> Pinecone 데이터 동기화
    // =================================================================================
    public void syncMysqlToPinecone(int startPage) {
        int page = startPage;
        int batchSize = 10;
        log.info("=========== [Sync] GPT 데이터 보강 시작 (Page: {}) ===========", page);

        while (true) {
            int offset = page * batchSize;

            // MyBatis Mapper 호출 (DTO 리스트 반환)
            List<AttractionResponse2> attractions = attractionMapper.findRagData(batchSize, offset);

            if (attractions.isEmpty()) break;

            try {
                List<String> embedTexts = new ArrayList<>();
                List<String> idList = new ArrayList<>();
                List<Map<String, Object>> metadataList = new ArrayList<>();

                for (AttractionResponse2 attr : attractions) {
                    // DTO에서 데이터 추출
                    String title = attr.getTitle();
                    String addr = (attr.getAddress() != null) ? attr.getAddress() : "주소 미상";
                    String category = (attr.getContentTypeName() != null) ? attr.getContentTypeName() : "관광지";
                    String overview = (attr.getOverview() != null) ? attr.getOverview() : "";

                    // 프롬프트 생성 (이전과 동일)
                    String prompt = String.format(
                            "데이터 정보 - [명칭: %s, 위치: %s, 유형: %s, 설명: %s]\n\n" +
                                    "당신은 검색 엔진 최적화를 위한 데이터 태깅 AI입니다.\n" +
                                    "사용자가 여행이나 나들이 계획을 짤 때 검색할 만한 **'목적'과 '상황'**을 문장으로 추출하세요.\n" +
                                    "## 1. 위치 기반 문맥: 도심형 vs 목적형 여행지 구분\n" +
                                    "## 2. 활동 추론: 유형과 명칭을 보고 할 수 있는 행동 서술\n" +
                                    "## 3. 출력 형식: 3개의 자연어 문장 (지리적 문맥, 핵심 활동, 동반자 및 분위기)",
                            title, addr, category, overview
                    );

                    String enrichedText = callChatGpt("검색 최적화 전략가", prompt, 0.3);
                    String finalSearchText = String.format("명칭: %s. 위치: %s. %s", title, addr, enrichedText);

                    embedTexts.add(finalSearchText);
                    // Pinecone ID는 String이어야 함
                    idList.add(String.valueOf(attr.getAttractionId()));

                    metadataList.add(Map.of(
                            "attractionId", attr.getAttractionId(),
                            "title", title,
                            "text", finalSearchText
                    ));
                }

                List<List<Double>> vectors = getOpenAiEmbeddingBatch(embedTexts);
                upsertToPineconeBatch(idList, vectors, metadataList);

                log.info(">> [Sync] {} 페이지 가공 및 저장 완료 (Offset: {})", page, offset);
                Thread.sleep(1000); // API 속도 제한 고려
            } catch (Exception e) {
                log.error("!! [Sync] 에러: {}", e.getMessage());
                break;
            }
            page++;
        }
    }

    // =================================================================================
    // 2. [Chat] RAG 채팅 기능
    // =================================================================================
    public AiResponse chat(String message) {
        String refinedQuery = refineQuery(message);
        log.info("[Chat] 정제된 검색어: {}", refinedQuery);

        List<Double> vector = getOpenAiEmbedding(refinedQuery);
        List<Integer> ids = searchPinecone(vector, 20);

        if (ids.isEmpty()) return new AiResponse("죄송합니다. 관련 장소를 찾지 못했습니다.", new ArrayList<>());

        // [변경] Mapper를 사용하여 ID 리스트로 조회
        List<AttractionResponse2> candidates = attractionMapper.findAllByIds(ids);

        // 검색 컨텍스트 생성
        String context = candidates.stream().limit(6)
                .map(p -> String.format("ID:%d, 제목:%s, 주소:%s, 설명:%s",
                        p.getAttractionId(), p.getTitle(), p.getAddress(),
                        (p.getOverview() != null ? p.getOverview().substring(0, Math.min(p.getOverview().length(), 100)) : "설명 없음")))
                .collect(Collectors.joining("\n\n"));

        String sysMsg = "당신은 한국 여행 가이드입니다. 목록의 정보만 사용하여 답변하세요.\n" +
                "1. 추천하는 각 장소 이름 옆에 반드시 (ID:번호)를 붙이세요. 예: 불국사 (ID:456)\n" +
                "2. 본문에는 [ID: 123] 형식을 쓰지 말고 반드시 (ID:123) 형식을 쓰세요.\n" +
                "3. 답변 마지막 줄에만 [IDS: 123, 456] 형식을 반드시 포함하세요.";

        String userMsg = String.format("[추천 후보 목록]\n%s\n\n질문: %s", context, message);
        String rawAnswer = callChatGpt(sysMsg, userMsg, 0.5);

        return parseResponse(rawAnswer, candidates);
    }

    // =================================================================================
    // 유틸리티 메서드 (기존과 동일)
    // =================================================================================
    private String refineQuery(String msg) {
        String sys = "검색 의도 분석기입니다. 질문에서 1.지역 2.방문목적 3.환경 키워드를 추출하세요.";
        return callChatGpt("검색어 정제기", sys + "\n질문: " + msg, 0.1);
    }

    private AiResponse parseResponse(String raw, List<AttractionResponse2> candidates) {
        Set<Integer> selectedIds = new HashSet<>();

        String aiMessage = raw;
        if (raw.contains("[IDS:")) {
            int idx = raw.lastIndexOf("[IDS:");
            aiMessage = raw.substring(0, idx).trim();
            try {
                String idPart = raw.substring(idx + 5).replace("]", "").trim();
                for (String s : idPart.split(",")) {
                    selectedIds.add(Integer.parseInt(s.trim()));
                }
            } catch (Exception e) {
                log.warn("IDS 태그 파싱 에러");
            }
        }

        Pattern idPattern = Pattern.compile("\\(ID:(\\d+)\\)");
        Matcher matcher = idPattern.matcher(aiMessage);
        while (matcher.find()) {
            selectedIds.add(Integer.parseInt(matcher.group(1)));
        }

        aiMessage = matcher.replaceAll("").replaceAll("\\s{2,}", " ").trim();

        // DTO 리스트에서 필터링
        List<AiResponse.PlaceInfo> places = candidates.stream()
                .filter(c -> selectedIds.contains(c.getAttractionId()))
                .map(c -> new AiResponse.PlaceInfo(
                        c.getAttractionId(), // PlaceInfo가 Long을 쓴다면 변환
                        c.getTitle(),
                        c.getAddress(),
                        c.getImageUrl()))
                .collect(Collectors.toList());

        return new AiResponse(aiMessage, places);
    }

    private String callChatGpt(String role, String content, double temp) {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", role),
                        Map.of("role", "user", "content", content)
                ),
                "temperature", temp
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, createHeaders(openAiKey)), Map.class);
            Map respBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("GPT 호출 실패: {}", e.getMessage());
            return "오류 발생";
        }
    }

    // Embedding 및 Pinecone 관련 메서드들 (기존 코드 그대로 유지)
    private List<List<Double>> getOpenAiEmbeddingBatch(List<String> texts) {
        String url = "https://api.openai.com/v1/embeddings";
        Map<String, Object> body = Map.of("model", "text-embedding-3-small", "input", texts);
        Map resp = restTemplate.postForObject(url, new HttpEntity<>(body, createHeaders(openAiKey)), Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
        return data.stream().map(d -> (List<Double>) d.get("embedding")).collect(Collectors.toList());
    }

    private List<Double> getOpenAiEmbedding(String text) { return getOpenAiEmbeddingBatch(List.of(text)).get(0); }

    private void upsertToPineconeBatch(List<String> ids, List<List<Double>> vectors, List<Map<String, Object>> metas) {
        String url = pineconeHost + "/vectors/upsert";
        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            payload.add(Map.of("id", ids.get(i), "values", vectors.get(i), "metadata", metas.get(i)));
        }
        restTemplate.postForEntity(url, new HttpEntity<>(Map.of("vectors", payload), createPineconeHeaders()), String.class);
    }

    private List<Integer> searchPinecone(List<Double> vector, int topK) {
        String url = pineconeHost + "/query";
        Map<String, Object> body = Map.of("vector", vector, "topK", topK, "includeMetadata", true);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, createPineconeHeaders()), Map.class);
        List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.getBody().get("matches");
        return matches.stream()
                .map(m -> {
                    Map<String, Object> metadata = (Map<String, Object>) m.get("metadata");
                    // Pinecone에서 메타데이터는 숫자여도 Double/String 등으로 올 수 있어 안전하게 파싱
                    return Integer.parseInt(metadata.get("attractionId").toString());
                })
                .collect(Collectors.toList());
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