package com.practice.OAuth2.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserInfoRequest {

    @Getter
    @NoArgsConstructor
    public static class updateUserProfile{
        private String nickname;
        private String profileImageUrl;
    }
}
