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

    // =================================================================================
    // 1. [Chat] RAG 채팅 기능 (GPT가 픽한 장소만 정확히 리턴 + 지역 필터링)
    // =================================================================================
    public AiResponse chat(String message) {
        // 1. 임베딩 & 검색 (후보군을 넉넉히 15개 가져옴)
        List<Double> queryVector = getOpenAiEmbedding(message);
        List<Integer> attractionIds = searchPinecone(queryVector, 15);
        List<Attraction> candidates = attractionRepository.findAllById(attractionIds);

        if (candidates.isEmpty()) {
            return new AiResponse("죄송합니다. 관련 여행지를 찾을 수 없습니다.", new ArrayList<>());
        }

        // 2. 프롬프트 구성
        // 형식: ID: 123 | [카테고리] 제목 (주소): 설명
        String context = candidates.stream()
                .map(p -> {
                    String category = (p.getContentType() != null) ? p.getContentType().getContentTypeName() : "기타";
                    return String.format("ID: %d | [%s] %s (%s): %s",
                            p.getAttrId(), category, p.getTitle(), p.getAddr1(), p.getOverview());
                })
                .collect(Collectors.joining("\n\n"));

        // 3. 🚨 [핵심 명령] 답변 끝에 선택한 ID를 태그해달라고 지시
        String systemPrompt = "당신은 한국 여행 가이드입니다. 제공된 [여행지 목록] 중에서 사용자의 질문에 가장 적합한 장소를 3개 이내로 골라 답변하세요.\n" +
                "목록에 없는 장소는 절대 언급하지 마세요.\n" +
                "★중요: 답변을 모두 마친 후, 맨 마지막 줄에 당신이 추천한 장소의 ID를 반드시 다음 형식으로 적어주세요.\n" +
                "형식: [IDS: 123, 456]";

        String userPrompt = String.format("[여행지 목록]\n%s\n\n사용자 질문: %s", context, message);

        // 4. GPT 호출
        String rawAnswer = callChatGpt(systemPrompt, userPrompt);

        // 5. ✂️ [파싱] 답변 텍스트와 ID 분리하기
        String aiMessage = rawAnswer;
        List<Integer> selectedIds = new ArrayList<>();

        int tagIndex = rawAnswer.lastIndexOf("[IDS:");

        if (tagIndex != -1) {
            // 태그 앞부분은 사용자에게 보여줄 메시지
            aiMessage = rawAnswer.substring(0, tagIndex).trim();

            // 태그 뒷부분에서 ID 추출
            try {
                String idsStr = rawAnswer.substring(tagIndex + 5).replace("]", "").trim();
                if (!idsStr.isBlank()) {
                    String[] idArray = idsStr.split(",");
                    for (String id : idArray) {
                        selectedIds.add(Integer.parseInt(id.trim()));
                    }
                }
            } catch (Exception e) {
                log.error("ID 파싱 중 오류 (형식 불일치): {}", rawAnswer);
                // 파싱 실패 시, 상위 3개 사용 (Fallback)
                selectedIds = attractionIds.subList(0, Math.min(attractionIds.size(), 3));
            }
        } else {
            // 태그가 없으면 상위 3개 보여줌
            selectedIds = attractionIds.subList(0, Math.min(attractionIds.size(), 3));
        }

        // 6. 최종 필터링: GPT가 선택한 ID만 리스트에 담기
        List<Integer> finalSelectedIds = selectedIds;
        List<AiResponse.PlaceInfo> placeInfos = candidates.stream()
                .filter(p -> finalSelectedIds.contains(p.getAttrId()))
                .map(p -> new AiResponse.PlaceInfo(
                        p.getAttrId(), p.getTitle(), p.getAddr1(), p.getFirstImage1()))
                .collect(Collectors.toList());

        return new AiResponse(aiMessage, placeInfos);
    }

    // =================================================================================
    // 2. [Sync] 데이터 동기화 (MySQL -> Pinecone)
    // ★수정됨★: startPage 파라미터를 받아서 중단된 곳부터 시작 가능
    // =================================================================================
    public void syncMysqlToPinecone(int startPage) {
        int page = startPage; // 전달받은 페이지부터 시작 (예: 22)
        int batchSize = 50;
        log.info("=========== OpenAI 배치 동기화 시작 (Start Page: {}, Batch: {}) ===========", page, batchSize);

        while (true) {
            Page<Attraction> attractionPage = attractionRepository.findRagData(PageRequest.of(page, batchSize));
            if (attractionPage.isEmpty()) break;

            List<Attraction> attractions = attractionPage.getContent();

            try {
                List<String> textList = new ArrayList<>();
                List<String> idList = new ArrayList<>();

                for (Attraction attr : attractions) {
                    String category = "기타";
                    if (attr.getContentType() != null) {
                        category = attr.getContentType().getContentTypeName();
                    }

                    String address = attr.getAddr1();
                    if (address == null) address = "";

                    // 🥪 [샌드위치 기법] 주소 재강조 (위치: %s)
                    String rawText = String.format("[%s] %s. %s - %s (위치: %s)",
                            category,
                            address,
                            attr.getTitle(),
                            attr.getOverview() != null ? attr.getOverview() : "",
                            address
                    );

                    String safeText = rawText.length() > 2000 ? rawText.substring(0, 2000) : rawText;

                    textList.add(safeText);
                    idList.add(attr.getAttrId().toString());
                }

                // API 호출 (배치)
                List<List<Double>> embeddings = getOpenAiEmbeddingBatch(textList);
                upsertToPineconeBatch(idList, embeddings, textList);

                log.info(">> {} 페이지 완료 (ID: {} ~ {})", page, idList.get(0), idList.get(idList.size()-1));

                // [안전 장치] 2초 대기
                Thread.sleep(2000);

            } catch (Exception e) {
                log.error("!! {} 페이지 에러: {}", page, e.getMessage());
                // 에러 나면 10초 쉬고 다음 페이지로 (멈춤 방지)
                try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
            }
            page++;
        }
        log.info("=========== 동기화 작업 종료 ===========");
    }

    // =================================================================================
    // [Helper Methods] API 통신 로직
    // =================================================================================

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

    private List<Integer> searchPinecone(List<Double> vector, int topK) {
        String url = pineconeHost + "/query";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", pineconeKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> request = Map.of("vector", vector, "topK", topK, "includeMetadata", true);
        HttpEntity<Map> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() == null || !response.getBody().containsKey("matches")) return new ArrayList<>();
            List<Map> matches = (List<Map>) response.getBody().get("matches");
            return matches.stream().map(m -> Integer.parseInt(((Map) m.get("metadata")).get("attractionId").toString())).collect(Collectors.toList());
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}