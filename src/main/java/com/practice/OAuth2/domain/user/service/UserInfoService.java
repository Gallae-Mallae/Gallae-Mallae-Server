package com.practice.OAuth2.domain.user.service;

import com.practice.OAuth2.domain.user.dto.UserInfoRequest;
import com.practice.OAuth2.domain.user.dto.UserInfoResponse;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.exception.ResourceNotFoundException;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserInfoService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return UserInfoResponse.from(user);
    }

    // 닉네임 수정
    public UserInfoResponse updateUserProfile(UserPrincipal userPrincipal, UserInfoRequest.updateUserProfile req  ){
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        user.updateUserProfile(req.getNickname() );
        return UserInfoResponse.from(user);
    }

    // 회원 탈퇴
    public void withdraw(UserPrincipal userPrincipal){
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        userRepository.delete(user);
    }
}
