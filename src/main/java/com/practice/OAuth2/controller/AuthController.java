package com.practice.OAuth2.controller;

import com.practice.OAuth2.config.AppProperties;
import com.practice.OAuth2.exception.BadRequestException;
import com.practice.OAuth2.model.AuthProvider;
import com.practice.OAuth2.model.RefreshToken;
import com.practice.OAuth2.model.User;
import com.practice.OAuth2.payload.ApiResponse;
import com.practice.OAuth2.payload.AuthResponse;
import com.practice.OAuth2.payload.LoginRequest;
import com.practice.OAuth2.payload.SignUpRequest;
import com.practice.OAuth2.repository.RefreshTokenRepository;
import com.practice.OAuth2.repository.UserRepository;
import com.practice.OAuth2.security.TokenProvider;
import com.practice.OAuth2.security.UserPrincipal;
import com.practice.OAuth2.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {  // 우리 서비스 자체 로그인 시스템 API ( 리프레쉬 관련 재발급 코드, 로그아웃 코드 포함)

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;

    private final RefreshTokenRepository refreshTokenRepository;

    private final AppProperties appProperties;
    
    // 소셜 로그인 유저라도 Access Token(30분)이 만료되면 프런트엔드가 이 API를 호출해서 연명 치료를 해야함
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refresh_token 꺼내기
        String refreshToken = CookieUtils.getCookie(request, "refresh_token")
                .map(Cookie::getValue)
                .orElse(null);

        // 리프레시 토큰 존재 여부
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰이 없습니다.");
        }

        // 리프레시토큰 유효성 검사 체크
        if (!tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시 토큰입니다.");
        }

        // 브라우저에게 쿠키로 받은 리프레시 토큰과 Redis 저장소(TTL: 14일)에 있는 리프레시토큰을 비교
        String userId = String.valueOf(tokenProvider.getUserIdFromToken(refreshToken));
        
        // 속도를 위해 PK (id) 사용
        RefreshToken redisToken = refreshTokenRepository.findById(userId).orElse(null);

        if (redisToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰의 유효기간이 만료되었습니다.");
        }

        if (!redisToken.getToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("잘못된 리프레시 토큰입니다.");
        }

        // 새 토큰 생성을 위한 Authentication 객체 생성 (ID만 있으면 됨)
        // TokenProvider는 토큰을 만들 때 Authentication 안에 있는 UserPrincipal을 꺼내고, 그 안의 id를 써서 토큰을 만들도록 되어있음
        UserDetails principal = UserPrincipal.create(Long.parseLong(userId));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

        // 새 토큰 발급 (RTR: Refresh Token도 새로 발급)
        String newAccessToken = tokenProvider.createToken(authentication);
        String newRefreshToken = tokenProvider.createRefreshToken(authentication);

        // 6. Redis 업데이트
        refreshTokenRepository.save(new RefreshToken(userId, newRefreshToken));

        // 7. 쿠키 갱신
        long accessTokenExpiry = appProperties.getAuth().getTokenExpirationMsec() / 1000;
        long refreshTokenExpiry = 1209600; // 14일

        // 응답 헤더에 쿠키를 Set-Cookie (새로 발급받은 엑세스,리프레시 토큰)
        setTokenCookies(response, newAccessToken, newRefreshToken, accessTokenExpiry, refreshTokenExpiry);

        return ResponseEntity.status(HttpStatus.OK).body("토큰 재발급 완료.");
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {


        // 브라우저가 보낸 쿠키 중 리프레시 토큰 조회
        String refreshToken = CookieUtils.getCookie(request, "refresh_token").map(Cookie::getValue).orElse(null);

        // 리프레시 토큰이 있고 이게 유효하다면 -> 레디스캐시 Repository의 리프레시 토큰 삭제
        if (refreshToken != null && tokenProvider.validateToken(refreshToken)) {
            String userId = String.valueOf(tokenProvider.getUserIdFromToken(refreshToken));
            refreshTokenRepository.deleteById(userId);
        }

        // 응답 헤더에 쿠키를 Set-Cookie (Max-Age = 0)
        setTokenCookies(response, "", "", 0, 0);

        return new ResponseEntity<>("로그아웃 되었습니다", HttpStatus.OK);
    }

    // 클라이언트에게 헤더에 응답쿠키를 설정하여 응답하는 메서드 -> 브라우저가 관리할 쿠키 설정
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken, long accessAge, long refreshAge) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(accessAge)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(refreshAge)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.createToken(authentication);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        if(userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email address already in use.");
        }

        // Creating user's account
        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword());
        user.setProvider(AuthProvider.local);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User result = userRepository.save(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/user/me")
                .buildAndExpand(result.getUserId()).toUri();

        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "User registered successfully@"));
    }

}
