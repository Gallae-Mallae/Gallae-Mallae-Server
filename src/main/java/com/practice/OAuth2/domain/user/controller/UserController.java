package com.practice.OAuth2.domain.user.controller;

import com.practice.OAuth2.domain.user.dto.UserInfoResponse;
import com.practice.OAuth2.domain.user.service.UserInfoService;
import com.practice.OAuth2.global.exception.ResourceNotFoundException;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.security.CurrentUser;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserInfoService userInfoService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {

        return ResponseEntity.status(HttpStatus.OK).
                body(userInfoService.getCurrentUser(userPrincipal));
    }
}
