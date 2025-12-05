package com.practice.OAuth2.domain.plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PlanWebSocketController {
    // 메시지 브로커로 메시지 전달하는 핵심 도구
    private final SimpMessagingTemplate template;


}
