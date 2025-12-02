package com.practice.OAuth2.domain.user.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter @Setter
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
public class User extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String name;

    private String nickname;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    //private String imageUrl;
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @JsonIgnore
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    //------------------------
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
