package com.practice.OAuth2.domain.ai.controller;

import com.practice.OAuth2.domain.ai.dto.AiResponse;
import com.practice.OAuth2.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // 1. 데이터 동기화 (관리자용)
    @PostMapping("/sync")
    public ResponseEntity<String> sync() {
        aiService.syncMysqlToPinecone(98);
        return ResponseEntity.ok("동기화 시작됨");
    }

    // 2. RAG 채팅 (사용자용)
    @GetMapping("/chat")
    public ResponseEntity<AiResponse> chat(@RequestParam String message) {
        return ResponseEntity.ok(aiService.chat(message));
    }
}