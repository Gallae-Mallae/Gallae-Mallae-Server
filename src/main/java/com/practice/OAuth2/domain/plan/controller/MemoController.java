package com.practice.OAuth2.domain.plan.controller;

import com.practice.OAuth2.domain.plan.dto.MemoRequest;
import com.practice.OAuth2.domain.plan.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memos") // ★ 여기를 주목! 모든 요청의 시작점
public class MemoController {

    private final MemoService memoService;

    // 메모 생성
    @PostMapping
    public ResponseEntity<Void> createMemo(@RequestBody MemoRequest request) {
        // blockId를 DTO에서 꺼내서 넘김
        memoService.createMemo(request.getBlockId(), request);
        return ResponseEntity.ok().build();
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