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
    private String description;
    private String folderImageUrl;
    
    public static ScrapFolderResponse from(ScrapFolder folder) {
        return ScrapFolderResponse.builder()
                .folderId(folder.getFolderId())
                .name(folder.getName())
                .description(folder.getDescription())
                .folderImageUrl(folder.getFolderImageUrl())
                .build();
    }
}
