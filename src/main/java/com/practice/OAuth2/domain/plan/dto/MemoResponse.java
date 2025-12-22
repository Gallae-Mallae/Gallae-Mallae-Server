package com.practice.OAuth2.domain.plan.dto;

import com.practice.OAuth2.domain.plan.entity.Memo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemoResponse {
    private Long memoId;
    private Long blockId;
    private String content;
    private String linkUrl;
    private String type;
    private Integer orderIndex;

    // Entity -> DTO 변환 생성자
    public MemoResponse(Memo memo) {
        this.memoId = memo.getMemoId();
        this.blockId = memo.getScheduleBlock().getBlockId();
        this.content = memo.getContent();
        this.linkUrl = memo.getLinkUrl();
        this.type = memo.getType();
        this.orderIndex = memo.getOrderIndex();
    }
}