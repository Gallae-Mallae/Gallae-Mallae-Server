package com.practice.OAuth2.global.security.oauth2;

import com.practice.OAuth2.global.exception.OAuth2AuthenticationProcessingException;
import com.practice.OAuth2.domain.user.entity.AuthProvider;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.security.UserPrincipal;
import com.practice.OAuth2.global.security.oauth2.user.OAuth2UserInfo;
import com.practice.OAuth2.global.security.oauth2.user.OAuth2UserInfoFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    //백엔드 리다이렉션 페이지에서 토큰을 받은 후 리소스에 다시 요청해서 유저 정보를 받아옴
    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2UserRequest, oAuth2User.getAttributes());

        if(StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if(userOptional.isPresent()) {
            user = userOptional.get();
            if(!user.getProvider().equals(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))) {
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // (추가) 탈퇴한 유저인지 확인
            user = restoreIfDeleted(oAuth2UserInfo.getEmail());

            if (user != null) {
                // 탈퇴한 유저 복구 후 업데이트
                user = updateExistingUser(user, oAuth2UserInfo);
            } else {
                // 아예 없는 유저 -> 신규 가입
                user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
            }
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    // (추가) 탈퇴한 유저 복구 메서드
    private User restoreIfDeleted(String email) {
        // 복구 쿼리 실행 (업데이트된 행의 개수 반환)
        int updatedCount = userRepository.restoreUser(email);

        if (updatedCount > 0) {
            // 복구됐으면(1건 이상 업데이트) 해당 유저 정보를 조회해서 반환
            // 이제 deleted_at이 NULL이 되었으므로 findByEmail로 조회가 가능
            return userRepository.findByEmail(email).orElse(null);
        }
        return null; // 복구할 대상이 x-> 신규 가입 필요
    }

    // 빌더패턴으로 수정
    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider provider = AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId());

        User user = User.builder()
                .provider(provider)
                .providerId(oAuth2UserInfo.getId())
                .name(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .profileImageUrl(oAuth2UserInfo.getImageUrl())
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }

}
