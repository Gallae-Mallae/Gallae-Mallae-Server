package com.practice.OAuth2.global.security.oauth2;

import com.practice.OAuth2.global.config.AppProperties;
import com.practice.OAuth2.global.exception.BadRequestException;
import com.practice.OAuth2.domain.auth.entity.RefreshToken;
import com.practice.OAuth2.domain.auth.repository.RefreshTokenRepository;
import com.practice.OAuth2.global.security.TokenProvider;
import com.practice.OAuth2.global.security.UserPrincipal;
import com.practice.OAuth2.global.util.CookieUtils;
import com.practice.OAuth2.global.util.TokenCookieManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.practice.OAuth2.global.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler { // 소셜로그인 API

    private final TokenProvider tokenProvider;

    private final AppProperties appProperties;

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    private final RefreshTokenRepository refreshTokenRepository; // Redis Repository 추가

    private final TokenCookieManager tokenCookieManager;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        // 엑세스 토큰 생성
        String accessToken = tokenProvider.createToken(authentication);
        
        // 리프레시 토큰 생성
        String refreshToken = tokenProvider.createRefreshToken(authentication);

        // Redis에 Refresh Token 저장
        // UserPrincipal에서 ID를 꺼내와서 Key로 사용
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal(); // Spring Security가 관리하는 현재 로그인한 사용자 정보(Object)를 우리가 만든 커스텀 객체(UserPrincipal)로 변환
        refreshTokenRepository.save(new RefreshToken(Long.toString(userPrincipal.getId()), refreshToken));

        // 쿠키 시간 설정
        long accessTokenExpiry = appProperties.getAuth().getTokenExpirationMsec() / 1000;
        long refreshTokenExpiry = 1209600; // 14일

        // 응답 헤더에 쿠키를 Set-Cookie (새로 발급받은 엑세스,리프레시 토큰)
        tokenCookieManager.addTokenCookies(response, accessToken,
                refreshToken, accessTokenExpiry, refreshTokenExpiry);

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if(redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new BadRequestException("Sorry! We've got an Unauthorized Redirect URI and can't proceed with the authentication");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        return UriComponentsBuilder.fromUriString(targetUrl)
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return appProperties.getOauth2().getAuthorizedRedirectUris()
                .stream()
                .anyMatch(authorizedRedirectUri -> {
                    // Only validate host and port. Let the clients use different paths if they want to
                    URI authorizedURI = URI.create(authorizedRedirectUri);
                    if(authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                            && authorizedURI.getPort() == clientRedirectUri.getPort()) {
                        return true;
                    }
                    return false;
                });
    }
}
