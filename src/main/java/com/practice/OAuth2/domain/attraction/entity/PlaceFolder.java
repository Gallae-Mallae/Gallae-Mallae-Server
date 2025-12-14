package com.practice.OAuth2.domain.attraction.entity;

import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "place_folders")
public class PlaceFolder extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String color;

    @OneToMany(mappedBy = "placeFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConnFolderPlace> connFolderPlaces = new ArrayList<>();

    @Builder
    public PlaceFolder(User user, String name, String color) {
        this.user = user;
        this.name = name;
        this.color = color;
    }
}
