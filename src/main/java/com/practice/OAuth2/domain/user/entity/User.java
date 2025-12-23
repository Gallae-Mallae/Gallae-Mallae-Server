package com.practice.OAuth2.domain.user.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;


@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@Where(clause = "deleted_at IS NULL")
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
public class User extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String name;

    private String nickname;

    private String password;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String name, String nickname, AuthProvider provider, String providerId, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.profileImageUrl = profileImageUrl;
        this.emailVerified = false;
    }

    public void updateUserProfile(String nickname) {
        this.nickname = nickname;
    }
}
