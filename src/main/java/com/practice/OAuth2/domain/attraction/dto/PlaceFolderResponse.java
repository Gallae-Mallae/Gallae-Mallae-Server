package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Getter
public class PlaceFolderResponse {
    private String name;
    private String color;
    private Integer counts;

    @Builder
    public PlaceFolderResponse(String name, String color, Integer counts) {
        this.name = name;
        this.color = color;
        this.counts = counts;
    }

    public static PlaceFolderResponse from(PlaceFolder placeFolder) {
        return PlaceFolderResponse.builder()
                .name(placeFolder.getName())
                .color(placeFolder.getColor())
                .counts(placeFolder.getConnFolderPlaces().size())
                .build();
    }
}
