package com.practice.OAuth2.domain.user.service;

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
public class UserInfoService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return UserInfoResponse.from(user);
    }


}
