package com.practice.OAuth2.domain.attraction.entity;

import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "place_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"attr_id", "user_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class PlaceLike extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_id", nullable = false)
    private Attraction attraction;

    public PlaceLike(User user, Attraction attraction) {
        this.user = user;
        this.attraction = attraction;
    }

}
