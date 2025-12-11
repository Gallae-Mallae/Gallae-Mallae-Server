package com.practice.OAuth2.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 서버에 핸드셰이크할 엔드포인트 (웹소켓 연결 시작 URL)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        // 포스트맨으로 웹소켓 테스트시 SockJS 주석 필요
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 서버 구독하고, 서버가 클라이언트에게 메시지 발행하는 Prefix
        registry.enableSimpleBroker("/topic");

        // 클라이언트에서 서버로 메시지 보내는 Prefix (@MessageMapping 핸들러로 라우팅)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
