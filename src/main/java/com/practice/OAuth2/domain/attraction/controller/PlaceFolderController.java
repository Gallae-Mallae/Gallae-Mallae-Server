package com.practice.OAuth2.domain.attraction.controller;

import com.practice.OAuth2.domain.attraction.dto.PlaceFolderCreateRequest;
import com.practice.OAuth2.domain.attraction.dto.PlaceFolderInfoResponse;
import com.practice.OAuth2.domain.attraction.dto.PlaceFolderResponse;
import com.practice.OAuth2.domain.attraction.service.PlaceFolderService;
import com.practice.OAuth2.global.security.CurrentUser;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place_folders")
public class PlaceFolderController {

    private final PlaceFolderService placeFolderService;

    @PostMapping
    public ResponseEntity<String> createFolder(@CurrentUser UserPrincipal userPrincipal,
                                          @RequestBody PlaceFolderCreateRequest request) {

        placeFolderService.createFolder(userPrincipal, request);

        return ResponseEntity.status(HttpStatus.CREATED).body("폴더 생성 완료");
    }

    @PostMapping("/{place_foldersId}/attractions/{attractionsId}")
    public ResponseEntity<String> addAttractionInFolder(@CurrentUser UserPrincipal userPrincipal,
                                                        @PathVariable("place_foldersId") Long pId,
                                                        @PathVariable("attractionsId") Integer aId) {
        placeFolderService.addAttractionInFolder(userPrincipal, pId, aId);

        return ResponseEntity.status(HttpStatus.CREATED).body("여행지 추가 완료");
    }

    @GetMapping
    public ResponseEntity<List<PlaceFolderResponse>> getFolderList(@CurrentUser UserPrincipal userPrincipal) {

        List<PlaceFolderResponse> response = placeFolderService.getFolderList(userPrincipal);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{place_foldersId}")
    public ResponseEntity<List<PlaceFolderInfoResponse>> getFolderInfo(@CurrentUser UserPrincipal userPrincipal,
                                                                 @PathVariable("place_foldersId") Long placeFolderId) {
        List<PlaceFolderInfoResponse> response = placeFolderService.getFolderInfo(userPrincipal ,placeFolderId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{place_foldersId}")
    public ResponseEntity<String> updateFolderName(@CurrentUser UserPrincipal userPrincipal,
                                                   @PathVariable("place_foldersId") Long placeFolderId,
                                                   @RequestBody PlaceFolderCreateRequest request) {

        placeFolderService.updateFolderName(userPrincipal, placeFolderId, request);

        return ResponseEntity.status(HttpStatus.OK).body("폴더명 변경 완료");
    }

    @DeleteMapping("/{place_foldersId}")
    public ResponseEntity<String> deleteFolder(@CurrentUser UserPrincipal userPrincipal,
                                               @PathVariable("place_foldersId") Long placeFolderId) {

        placeFolderService.deleteFolder(userPrincipal, placeFolderId);

        return ResponseEntity.status(HttpStatus.OK).body("폴더 삭제 완료");
    }

    @DeleteMapping("/{place_foldersId}/attractions/{attractionsId}")
    public ResponseEntity<String> deleteAttractionInFolder(@CurrentUser UserPrincipal userPrincipal,
                                                           @PathVariable("place_foldersId") Long placeFolderId,
                                                           @PathVariable("attractionsId") Integer attractionId) {

        placeFolderService.deleteAttractionInFolder(userPrincipal, placeFolderId, attractionId);

        return ResponseEntity.status(HttpStatus.OK).body("여행지 삭제 완료");
    }

}
