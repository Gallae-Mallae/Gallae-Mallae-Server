package com.practice.OAuth2.domain.attraction.dto;

import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import com.practice.OAuth2.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlaceFolderCreateRequest {
    private String name;
    private String color;

    public PlaceFolder toEntity(User user) {
        return PlaceFolder.builder()
                .user(user)
                .name(this.name)
                .color(this.color)
                .build();
    }
}
