package com.practice.OAuth2.domain.ai.controller;

import com.practice.OAuth2.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // 이거 호출하면 MySQL 데이터를 싹 긁어서 파인콘에 넣습니다.
    @PostMapping("/sync")
    public ResponseEntity<String> sync() {
        aiService.syncMysqlToPinecone();
        return ResponseEntity.ok("동기화가 시작되었습니다. 서버 로그를 확인하세요.");
    }
}