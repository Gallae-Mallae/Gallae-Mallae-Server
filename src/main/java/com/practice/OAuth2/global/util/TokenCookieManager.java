package com.practice.OAuth2.global.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class TokenCookieManager {

    private static final String ACCESS_TOKEN_NAME = "access_token";
    private static final String REFRESH_TOKEN_NAME = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    // 토큰 쿠키 굽기 (로그인, 재발급 용)
    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken, long accessAge, long refreshAge) {
        // Access Token
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_NAME, accessToken)
                .path("/")
                .httpOnly(true)
                .secure(false) // HTTPS 필수
                .maxAge(accessAge)
                .sameSite("None") // 프런트 배포 이전에 테스트용으로 lax 해제
                .build();

        // Refresh Token
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_NAME, refreshToken)
                .path(REFRESH_TOKEN_PATH) // 재발급 경로에만 전송
                .httpOnly(true)
                .secure(false)
                .maxAge(refreshAge) // 브라우저 꺼도 유지됨 (Persistent Cookie)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    // 토큰 쿠키 삭제 (로그아웃 용 : maxAge 가 0 인채로 Set-Cookie)
    public void deleteTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .sameSite("None")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_NAME, "")
                .path(REFRESH_TOKEN_PATH)
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
