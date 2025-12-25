package com.practice.OAuth2.domain.ai.service;

import com.practice.OAuth2.domain.ai.dto.AiResponse;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AttractionRepository attractionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key}")
    private String openAiKey;

    @Value("${ai.pinecone.api-key}")
    private String pineconeKey;

    @Value("${ai.pinecone.host}")
    private String pineconeHost;

    // ★ [설정] 유사도 기준값 (0.0 ~ 1.0)
    // 0.4 미만의 유사도를 가진 데이터는 "관련 없음"으로 간주하고 필터링합니다.
    private static final double SIMILARITY_THRESHOLD = 0.4;

    // =================================================================================
    // 1. [Chat] RAG 채팅 기능 (검색어 정제 로직 포함)
    // =================================================================================
    public AiResponse chat(String message) {

        // ★ [Step 1] 검색어 정제 (Query Refinement)
        // 사용자의 애매한 질문(예: "몸 지지고 싶어")을 검색하기 좋은 키워드(예: "찜질방, 온천, 사우나")로 변환
        String searchKeyword = refineSearchKeyword(message);
        log.info(">> 사용자 질문: '{}' -> 정제된 키워드: '{}'", message, searchKeyword);

        // [Step 2] 정제된 키워드로 임베딩 생성
        List<Double> queryVector = getOpenAiEmbedding(searchKeyword);

        // [Step 3] Pinecone 검색 (정제된 키워드 벡터 사용 + 유사도 0.4 필터링)
        List<Integer> attractionIds = searchPinecone(queryVector, 15);

        // [Step 4] 방어 로직 (검색 결과가 없거나 유사도가 낮아서 걸러진 경우)
        if (attractionIds.isEmpty()) {
            return new AiResponse("죄송합니다. 요청하신 내용과 관련된 여행지를 찾을 수 없습니다. (검색 키워드: " + searchKeyword + ")", new ArrayList<>());
        }

        // [Step 5] DB에서 상세 정보 조회
        List<Attraction> candidates = attractionRepository.findAllById(attractionIds);

        // [Step 6] 프롬프트 데이터 구성 (GPT가 읽을 정보)
        String context = candidates.stream()
                .map(p -> {
                    String category = (p.getContentType() != null) ? p.getContentType().getContentTypeName() : "기타";
                    return String.format("- ID: %d | [지역: %s / 분류: %s] %s\n  설명: %s",
                            p.getAttrId(), p.getAddr1(), category, p.getTitle(), p.getOverview());
                })
                .collect(Collectors.joining("\n\n"));

        // [Step 7] 최종 답변 생성 요청 (시스템 프롬프트)
        String systemPrompt = "당신은 솔직하고 친절한 '한국 여행 가이드'입니다.\n" +
                "제공된 [여행지 목록]을 바탕으로 사용자의 질문에 답변해야 합니다.\n\n" +

                "[지침]\n" +
                "1. **적합성 판단**: 사용자의 질문과 [여행지 목록]이 관련이 있는지 확인하세요.\n" +
                "2. **거절하기**: 관련이 없다면 억지로 추천하지 말고 '죄송합니다. 관련 장소를 찾을 수 없습니다.'라고 답하세요.\n" +
                "3. **추천하기**: 가장 적합한 곳을 **최대 2곳** 선정하여 설명하세요. (설명 중에는 ID나 기호를 노출하지 마세요)\n" +
                "4. **형식 준수**: 답변 맨 마지막 줄에 ID 포맷을 반드시 지키세요.\n\n" +

                "★필수 포맷:\n" +
                "답변 끝에 [IDS: 12345, 67890] 형식으로 ID를 적으세요.";

        // 사용자 질문은 원본(message)을 넘겨주어 자연스러운 대화 유도
        String userPrompt = String.format("[여행지 목록]\n%s\n\n사용자 질문: \"%s\"\n위 목록에서 가장 알맞은 곳을 추천해줘.", context, message);

        // [Step 8] GPT 호출
        String rawAnswer = callChatGpt(systemPrompt, userPrompt);
        log.info("GPT Final Answer: {}", rawAnswer);

        // [Step 9] 파싱 (답변 텍스트와 ID 분리)
        String aiMessage = rawAnswer;
        List<Integer> selectedIds = new ArrayList<>();
        int tagIndex = rawAnswer.lastIndexOf("[IDS:");

        if (tagIndex != -1) {
            aiMessage = rawAnswer.substring(0, tagIndex).trim();
            try {
                String idsStr = rawAnswer.substring(tagIndex + 5).replace("]", "").trim();
                if (!idsStr.isBlank()) {
                    String[] idArray = idsStr.split(",");
                    for (String id : idArray) {
                        selectedIds.add(Integer.parseInt(id.trim()));
                    }
                }
            } catch (Exception e) {
                log.error("ID 파싱 오류");
            }
        }

        // [Step 10] 최종 응답 생성
        List<Integer> finalSelectedIds = selectedIds;
        List<AiResponse.PlaceInfo> placeInfos = candidates.stream()
                .filter(p -> finalSelectedIds.contains(p.getAttrId()))
                .map(p -> new AiResponse.PlaceInfo(
                        p.getAttrId(), p.getTitle(), p.getAddr1(), p.getFirstImage1()))
                .collect(Collectors.toList());

        return new AiResponse(aiMessage, placeInfos);
    }

    // =================================================================================
    // ★ [New] 검색어 정제 메서드 (User Intent Analysis)
    // =================================================================================
    private String refineSearchKeyword(String userMessage) {
        String systemPrompt = "너는 '검색어 추출기'야. 사용자의 질문을 보고, 한국 여행 데이터베이스에서 검색하기 좋은 '핵심 키워드' 3~5개를 나열해줘.\n" +
                "동의어나 관련 단어도 포함해.\n" +
                "예시: '배고파' -> '맛집, 식당, 음식점, 먹거리'\n" +
                "예시: '조용한 곳' -> '공원, 산책로, 숲, 조용한 카페, 사찰'\n" +
                "잡담하지 말고 오직 키워드만 쉼표로 구분해서 줘.";

        // 간단한 호출이므로 기존 callChatGpt 재활용
        return callChatGpt(systemPrompt, userMessage);
    }

    // =================================================================================
    // 2. [Sync] 데이터 동기화 (MySQL -> Pinecone)
    // =================================================================================
    public void syncMysqlToPinecone(int startPage) {
        int page = startPage;
        int batchSize = 50;
        log.info("=========== OpenAI 배치 동기화 시작 (Start Page: {}) ===========", page);

        while (true) {
            Page<Attraction> attractionPage = attractionRepository.findRagData(PageRequest.of(page, batchSize));
            if (attractionPage.isEmpty()) break;

            List<Attraction> attractions = attractionPage.getContent();

            try {
                List<String> textList = new ArrayList<>();
                List<String> idList = new ArrayList<>();

                for (Attraction attr : attractions) {
                    String category = (attr.getContentType() != null) ? attr.getContentType().getContentTypeName() : "기타";
                    String address = (attr.getAddr1() != null) ? attr.getAddr1() : "";
                    String overview = (attr.getOverview() != null) ? attr.getOverview() : "";

                    overview = overview.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim();

                    // 검색 품질을 위한 [샌드위치 포맷]
                    String rawText = String.format("[지역: %s] %s | %s | %s [위치: %s]",
                            address, attr.getTitle(), category, overview, address);

                    String safeText = rawText.length() > 2000 ? rawText.substring(0, 2000) : rawText;

                    textList.add(safeText);
                    idList.add(attr.getAttrId().toString());
                }

                List<List<Double>> embeddings = getOpenAiEmbeddingBatch(textList);
                upsertToPineconeBatch(idList, embeddings, textList);

                log.info(">> {} 페이지 완료 ({}건)", page, idList.size());
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("!! {} 페이지 에러: {}", page, e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
            page++;
        }
        log.info("=========== 동기화 작업 종료 ===========");
    }

    // =================================================================================
    // [Helper Methods] API 통신 및 유틸리티
    // =================================================================================

    // ★ 수정된 검색 로직: Score 필터링 (0.4 이상만)
    private List<Integer> searchPinecone(List<Double> vector, int topK) {
        String url = pineconeHost + "/query";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", pineconeKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = Map.of(
                "vector", vector,
                "topK", topK,
                "includeMetadata", true,
                "includeValues", false
        );

        HttpEntity<Map> entity = new HttpEntity<>(request, headers);

        List<Integer> validIds = new ArrayList<>();

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("matches")) {
                List<Map> matches = (List<Map>) response.getBody().get("matches");

                for (Map match : matches) {
                    // 점수 확인 (Double 또는 Integer 형변환 안전장치)
                    Object scoreObj = match.get("score");
                    Double score = 0.0;
                    if (scoreObj instanceof Double) {
                        score = (Double) scoreObj;
                    } else if (scoreObj instanceof Integer) {
                        score = ((Integer) scoreObj).doubleValue();
                    }

                    // ★ 점수가 기준치(0.4) 이상인 경우에만 결과에 포함
                    if (score >= SIMILARITY_THRESHOLD) {
                        Map metadata = (Map) match.get("metadata");
                        Object idObj = metadata.get("attractionId");
                        validIds.add(Integer.parseInt(idObj.toString()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Pinecone Search Error: {}", e.getMessage());
        }

        return validIds;
    }

    private List<Double> getOpenAiEmbedding(String text) {
        String url = "https://api.openai.com/v1/embeddings";
        HttpHeaders headers = createHeaders(openAiKey);

        String safeText = text.length() > 2000 ? text.substring(0, 2000) : text;
        Map<String, Object> body = Map.of("model", "text-embedding-3-small", "input", safeText);
        HttpEntity<Map> entity = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(url, entity, Map.class);
            List<Map> data = (List<Map>) response.get("data");
            return (List<Double>) data.get(0).get("embedding");
        } catch (Exception e) {
            throw new RuntimeException("OpenAI Error: " + e.getMessage());
        }
    }

    private List<List<Double>> getOpenAiEmbeddingBatch(List<String> texts) {
        String url = "https://api.openai.com/v1/embeddings";
        HttpHeaders headers = createHeaders(openAiKey);

        Map<String, Object> body = Map.of("model", "text-embedding-3-small", "input", texts);
        HttpEntity<Map> entity = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(url, entity, Map.class);
            List<Map> data = (List<Map>) response.get("data");
            return data.stream()
                    .map(item -> (List<Double>) item.get("embedding"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("OpenAI Batch Error: {}", e.getMessage());
            throw new RuntimeException("OpenAI Batch Error");
        }
    }

    private void upsertToPineconeBatch(List<String> ids, List<List<Double>> vectors, List<String> texts) {
        String url = pineconeHost + "/vectors/upsert";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", pineconeKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> vectorsPayload = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Map<String, Object> metadata = Map.of(
                    "attractionId", Integer.parseInt(ids.get(i)),
                    "text", texts.get(i).length() > 1000 ? texts.get(i).substring(0, 1000) : texts.get(i)
            );
            vectorsPayload.add(Map.of("id", ids.get(i), "values", vectors.get(i), "metadata", metadata));
        }

        Map<String, Object> request = Map.of("vectors", vectorsPayload);
        HttpEntity<Map> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("Pinecone Upsert Error: {}", e.getMessage());
            throw new RuntimeException("Pinecone Upsert Error");
        }
    }

    private String callChatGpt(String systemMsg, String userMsg) {
        String url = "https://api.openai.com/v1/chat/completions";
        HttpHeaders headers = createHeaders(openAiKey);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemMsg),
                Map.of("role", "user", "content", userMsg));
        Map<String, Object> body = Map.of("model", "gpt-3.5-turbo", "messages", messages, "temperature", 0.7);
        HttpEntity<Map> entity = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(url, entity, Map.class);
            List<Map> choices = (List<Map>) response.get("choices");
            Map message = (Map) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            return "AI Error";
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}