package com.practice.OAuth2.domain.user.dto;

import com.practice.OAuth2.domain.user.entity.AuthProvider;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.global.config.AppProperties.Auth;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder @Getter
public class UserInfoResponse {
    private Long userId;
    private String email;
    private String name;
    private String nickname;
    private String profileImageUrl;
    private AuthProvider provider;

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider())
                .build();
    }
}
