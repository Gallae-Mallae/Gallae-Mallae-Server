package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.MemoRequest;
import com.practice.OAuth2.domain.plan.dto.MemoResponse;
import com.practice.OAuth2.domain.plan.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memos") // ★ 여기를 주목! 모든 요청의 시작점
public class MemoController {

    private final MemoService memoService;

    // 메모 생성
    @PostMapping("/{blockId}")
    public ResponseEntity<Void> createMemo(
            @PathVariable Long blockId,
            @RequestBody MemoRequest request) {
        // blockId를 DTO에서 꺼내서 넘김
        memoService.createMemo(blockId, request);
        return ResponseEntity.ok().build();
    }

    // 메모 목록 조회
    @GetMapping("/{blockId}")
    public ResponseEntity<List<MemoResponse>> getMemos(
            @PathVariable Long blockId) {
        return ResponseEntity.ok(memoService.getMemos(blockId));
    }

    // 메모 수정
    @PatchMapping("/{memoId}")
    public ResponseEntity<Void> updateMemo(
            @PathVariable Long memoId,
            @RequestBody MemoRequest request
    ) {
        memoService.updateMemo(memoId, request);
        return ResponseEntity.ok().build();
    }

    // 메모 삭제
    @DeleteMapping("/{memoId}")
    public ResponseEntity<Void> deleteMemo(@PathVariable Long memoId) {
        memoService.deleteMemo(memoId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{blockId}/order")
    public ResponseEntity<Void> reorderMemos(
            @PathVariable Long blockId,
            @RequestBody List<Long> memoIds
    ) {
        memoService.reorderMemos(blockId, memoIds);
        return ResponseEntity.ok().build();
    }
}