package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Getter
public class PlaceFolderResponse {
    private Long placeFolderId;
    private String name;
    private String color;
    private Integer counts;

    @Builder
    public PlaceFolderResponse(Long placeFolderId, String name, String color, Integer counts) {
        this.placeFolderId = placeFolderId;
        this.name = name;
        this.color = color;
        this.counts = counts;
    }

    public static PlaceFolderResponse from(PlaceFolder placeFolder) {
        return PlaceFolderResponse.builder()
                .placeFolderId(placeFolder.getFolderId())
                .name(placeFolder.getName())
                .color(placeFolder.getColor())
                .counts(placeFolder.getConnFolderPlaces().size())
                .build();
    }
}
