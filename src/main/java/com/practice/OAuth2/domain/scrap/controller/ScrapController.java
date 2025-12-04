package com.practice.OAuth2.domain.scrap.controller;

import com.practice.OAuth2.domain.scrap.dto.ScrapReqest;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest.CreateScrap;
import com.practice.OAuth2.domain.scrap.dto.ScrapResponse;
import com.practice.OAuth2.domain.scrap.service.ScrapService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/scrap-folders")
public class ScrapController {

    private final ScrapService scrapService;

    // Http Response 받으려고 ResponseEntity사용함

    // 폴더 생성
    // 주소: POST /api/scrap-folders 임시
    @PostMapping()
    public ResponseEntity<Long> createScrapFolder(@RequestBody ScrapReqest.CreateScrapFolder req){

        // 임시 userId
        Long tempUserId = 1L;

        return ResponseEntity.ok(scrapService.createScrapFolder(tempUserId, req));
    }

    // 스크랩 생성 (폴더 안)
    // 주소: POST /api/scrap-folders/{folderId}/scraps 임시
    @PostMapping("/{folderId}/scraps")
    public ResponseEntity<Long> createScrapFolder(@PathVariable Long folderId, @RequestBody ScrapReqest.CreateScrap req){

        // 임시 userId
        Long tempUserId = 1L;

        return ResponseEntity.ok(scrapService.createScrap(tempUserId, folderId, req));
    }

    // 조회
    // 주소: GET /api/scrap-folders/{folderId}/scraps 임시
    @GetMapping("/{folderId}/scraps")
    public ResponseEntity<List<ScrapResponse>> getScraps(@PathVariable Long folderId){
        return ResponseEntity.ok(scrapService.getScraps(folderId));
    }

    // 폴더 수정
    // 주소: PATCH /api/scrap-folders/{folderId} 임시
    @PatchMapping("/{folderId}")
    public ResponseEntity<String> updateScrapFolder(
            @PathVariable Long folderId,
            @RequestBody ScrapReqest.UpdateScrapFolder req) {

        Long tempUserId = 1L; // 임시

        scrapService.updateScrapFolder(tempUserId, folderId, req);
        return ResponseEntity.ok("폴더가 수정되었습니다.");
    }

    // 폴더 삭제
    // 주소: DELETE /api/scrap-folders/{folderId} 임시
    @DeleteMapping("/{folderId}")
    public ResponseEntity<String> deleteScrapFolder(@PathVariable Long folderId) {

        Long tempUserId = 1L; // 임시

        scrapService.deleteScrapFolder(tempUserId, folderId);
        return ResponseEntity.ok("폴더가 삭제되었습니다.");
    }

    // 스크랩 수정
    // 주소: PATCH /api/scrap-folders/{folderId}/scraps/{scrapId} 임시
    @PatchMapping("/{folderId}/scraps/{scrapId}")
    public ResponseEntity<String> updateScrap(
            @PathVariable Long folderId,
            @PathVariable Long scrapId,
            @RequestBody ScrapReqest.UpdateScrap req) {

        Long tempUserId = 1L; // 임시

        scrapService.updateScrap(tempUserId, scrapId, req);
        return ResponseEntity.ok("스크랩이 수정되었습니다.");
    }

    // 스크랩 삭제
    // 주소: DELETE /api/scrap-folders/{folderId}/scraps/{scrapId} 임시
    @DeleteMapping("/{folderId}/scraps/{scrapId}")
    public ResponseEntity<String> deleteScrap(
            @PathVariable Long folderId,
            @PathVariable Long scrapId) {

        Long tempUserId = 1L; // 임시

        scrapService.deleteScrap(tempUserId, scrapId);
        return ResponseEntity.ok("스크랩이 삭제되었습니다.");
    }
}
