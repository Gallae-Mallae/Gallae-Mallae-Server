package com.practice.OAuth2.domain.attraction.controller;

import com.practice.OAuth2.domain.attraction.dto.PlaceFolderCreateRequest;
import com.practice.OAuth2.domain.attraction.service.PlaceFolderService;
import com.practice.OAuth2.global.security.CurrentUser;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place_folders")
public class PlaceFolderController {

    private final PlaceFolderService placeFolderService;

    @PostMapping
    public ResponseEntity<?> createFolder(@CurrentUser UserPrincipal userPrincipal,
                                          @RequestBody PlaceFolderCreateRequest request) {

        placeFolderService.createFolder(userPrincipal, request);

        return ResponseEntity.status(HttpStatus.CREATED).body("폴더 생성 완료");
    }
}
