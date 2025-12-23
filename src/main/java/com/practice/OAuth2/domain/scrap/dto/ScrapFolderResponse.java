package com.practice.OAuth2.domain.scrap.dto;

import com.practice.OAuth2.domain.scrap.entity.ScrapFolder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ScrapFolderResponse {
    private Long folderId;
    private String name;
    private String folderImageUrl;
    private Integer scrapCount;

    public ScrapFolderResponse(ScrapFolder folder, String latestImageUrl, Integer scrapCount) {
        this.folderId = folder.getFolderId(); // 엔티티 필드명 확인 (getId or getFolderId)
        this.name = folder.getName();
        this.scrapCount = scrapCount;

        // 이미지가 있으면 그 이미지를, 없으면 기본 이미지(placeholder)를 사용
        if (latestImageUrl != null && !latestImageUrl.isEmpty()) {
            this.folderImageUrl = latestImageUrl;
        } else {
            // [팁] 프론트엔드 프로젝트 안에 넣어둔 기본 이미지 경로 or S3의 기본 이미지 URL
            // 임시
            // this.folderImageUrl = "https://your-s3-bucket.com/static/default_folder_icon.png";
        }
    }
}
