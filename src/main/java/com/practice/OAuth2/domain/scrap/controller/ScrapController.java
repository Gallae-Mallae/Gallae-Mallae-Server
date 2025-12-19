package com.practice.OAuth2.domain.scrap.controller;

import com.practice.OAuth2.domain.scrap.dto.LinkMetadataResponse;
import com.practice.OAuth2.domain.scrap.dto.ScrapFolderResponse;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest.CreateScrap;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest.CreateScrapFolder;
import com.practice.OAuth2.domain.scrap.dto.ScrapResponse;
import com.practice.OAuth2.domain.scrap.service.ScrapService;
import com.practice.OAuth2.domain.scrap.service.UrlMetadataService;
import com.practice.OAuth2.global.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/scrap-folders")
public class ScrapController {

    private final ScrapService scrapService;

    // Http Response 받으려고 ResponseEntity사용함

    // 폴더 생성
    // 주소: POST /api/scrap-folders 임시
    @PostMapping()
    public ResponseEntity<Long> createScrapFolder(@AuthenticationPrincipal UserPrincipal principal, @RequestBody ScrapReqest.CreateScrapFolder req){

        return ResponseEntity.ok(scrapService.createScrapFolder(principal.getId(), req));
    }

    // 스크랩 생성 (폴더 안)
    // 주소: POST /api/scrap-folders/{folderId}/scraps 임시
    @PostMapping("/{folderId}/scraps")
    public ResponseEntity<Long> createScrap(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long folderId,
            @RequestBody ScrapReqest.CreateScrap req){

        return ResponseEntity.ok(scrapService.createScrap(principal.getId(), folderId, req));
    }

    // 폴더 조회
    @GetMapping()
    public ResponseEntity<List<ScrapFolderResponse>> getScrapFolder(
            @AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(scrapService.getScrapFolders(principal.getId()));
    }

    // 스크랩 조회
    // 주소: GET /api/scrap-folders/{folderId}/scraps 임시
    @GetMapping("/{folderId}/scraps")
    public ResponseEntity<List<ScrapResponse>> getScraps(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long folderId){
        return ResponseEntity.ok(scrapService.getScraps(principal.getId(), folderId));
    }

    // 폴더 수정
    // 주소: PATCH /api/scrap-folders/{folderId} 임시
    @PatchMapping("/{folderId}")
    public ResponseEntity<String> updateScrapFolder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long folderId,
            @RequestBody ScrapReqest.UpdateScrapFolder req) {


        scrapService.updateScrapFolder(principal.getId(), folderId, req);
        return ResponseEntity.ok("폴더가 수정되었습니다.");
    }

    // 폴더 삭제
    // 주소: DELETE /api/scrap-folders/{folderId} 임시
    @DeleteMapping("/{folderId}")
    public ResponseEntity<String> deleteScrapFolder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long folderId) {

        scrapService.deleteScrapFolder(principal.getId(), folderId);
        return ResponseEntity.ok("폴더가 삭제되었습니다.");
    }

    // 스크랩 수정
    // 주소: PATCH /api/scrap-folders/{folderId}/scraps/{scrapId} 임시
    @PatchMapping("/{folderId}/scraps/{scrapId}")
    public ResponseEntity<String> updateScrap(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long folderId,
            @PathVariable Long scrapId,
            @RequestBody ScrapReqest.UpdateScrap req) {

        scrapService.updateScrap(principal.getId(), scrapId, req);
        return ResponseEntity.ok("스크랩이 수정되었습니다.");
    }

    // 스크랩 삭제
    // 주소: DELETE /api/scrap-folders/{folderId}/scraps/{scrapId} 임시
    @DeleteMapping("/{folderId}/scraps/{scrapId}")
    public ResponseEntity<String> deleteScrap(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long folderId,
            @PathVariable Long scrapId) {

        scrapService.deleteScrap(principal.getId(), scrapId);
        return ResponseEntity.ok("스크랩이 삭제되었습니다.");
    }

    // 미리보기
    private final UrlMetadataService urlMetadataService;

    @GetMapping("/preview")
    public ResponseEntity<LinkMetadataResponse> getLinkPreview(@RequestParam String url) {
        LinkMetadataResponse response = urlMetadataService.extractMetadata(url);
        return ResponseEntity.ok(response);
    }
}
