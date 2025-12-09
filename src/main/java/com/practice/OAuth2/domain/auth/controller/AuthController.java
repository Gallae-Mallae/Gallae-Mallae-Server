package com.practice.OAuth2.domain.auth.controller;

import com.practice.OAuth2.domain.auth.dto.TokenResponse;
import com.practice.OAuth2.domain.auth.service.AuthService;
import com.practice.OAuth2.global.config.AppProperties;
import com.practice.OAuth2.global.exception.BadRequestException;
import com.practice.OAuth2.domain.user.entity.AuthProvider;
import com.practice.OAuth2.domain.auth.entity.RefreshToken;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.global.common.ApiResponse;
import com.practice.OAuth2.domain.auth.dto.AuthResponse;
import com.practice.OAuth2.domain.auth.dto.LoginRequest;
import com.practice.OAuth2.domain.auth.dto.SignUpRequest;
import com.practice.OAuth2.domain.auth.repository.RefreshTokenRepository;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.security.TokenProvider;
import com.practice.OAuth2.global.security.UserPrincipal;
import com.practice.OAuth2.global.util.CookieUtils;
import com.practice.OAuth2.global.util.TokenCookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenCookieManager tokenCookieManager;
    private final AppProperties appProperties;

    
    // 소셜 로그인 유저라도 Access Token(30분)이 만료되면 프런트엔드가 이 API를 호출해서 연명 치료를 해야함
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refresh_token 꺼내기
        String refreshToken = CookieUtils.getCookie(request, "refresh_token")
                .map(Cookie::getValue)
                .orElse(null);

        // 서비스단 예외 발생 시 GlobalHandler 로 처리
        TokenResponse tokenResponse = authService.reissue(refreshToken);

        // 쿠키 시간 설정
        long accessTokenExpiry = appProperties.getAuth().getTokenExpirationMsec() / 1000;
        long refreshTokenExpiry = 1209600; // 14일

        // 응답 헤더에 쿠키를 Set-Cookie (새로 발급받은 엑세스,리프레시 토큰)
        tokenCookieManager.addTokenCookies(response, tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(), accessTokenExpiry, refreshTokenExpiry);

        return ResponseEntity.status(HttpStatus.OK).body("토큰 재발급 완료.");
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtils.getCookie(request, "refresh_token")
                .map(Cookie::getValue)
                .orElse(null);

        authService.logout(refreshToken);

        tokenCookieManager.deleteTokenCookies(response);

        return ResponseEntity.ok().body("로그아웃 성공");
    }

}
