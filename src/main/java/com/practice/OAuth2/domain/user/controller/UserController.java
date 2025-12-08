package com.practice.OAuth2.domain.user.controller;

import com.practice.OAuth2.domain.user.dto.UserInfoRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserInfoService userInfoService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {

        return ResponseEntity.ok(userInfoService.getCurrentUser(userPrincipal));
    }

    // 닉네임 수정
    @PatchMapping("/me")
    public ResponseEntity<UserInfoResponse> updateUserProfile(@CurrentUser UserPrincipal userPrincipal,
            @RequestBody UserInfoRequest.updateUserProfile req) {

        return ResponseEntity.ok(userInfoService.updateUserProfile(userPrincipal, req));
    }

    // 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<String> withdraw(@CurrentUser UserPrincipal userPrincipal) {

        userInfoService.withdraw(userPrincipal);
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}
