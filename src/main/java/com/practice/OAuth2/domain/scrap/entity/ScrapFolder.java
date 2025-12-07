package com.practice.OAuth2.domain.scrap.entity;

import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "scrap_folders")
@SQLDelete(sql = "UPDATE scrap_folders SET deleted_at = NOW() WHERE folder_id = ?")
@Where(clause = "deleted_at IS NULL")
public class ScrapFolder extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "folder_image_url", length = 1000)
    private String folderImageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 생성자 추가
    @Builder
    public ScrapFolder(User user, String name, String description, String folderImageUrl){
        this.user = user;
        this.name = name;
        this.description = description;
        this.folderImageUrl = folderImageUrl;
    }

    // update ScrapFolder
    public void updateScrapFolder(String name, String description, String folderImageUrl) {
        this.name = name;
        this.description = description;
        this.folderImageUrl = folderImageUrl;
    }
}
