package com.practice.OAuth2.domain.user.controller;

import com.practice.OAuth2.global.exception.ResourceNotFoundException;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.security.CurrentUser;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public User getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
    }
}
