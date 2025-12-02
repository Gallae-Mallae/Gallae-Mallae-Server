package com.practice.OAuth2.domain.plan.entity;

import com.practice.OAuth2.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "plans")
@SQLDelete(sql = "UPDATE plans SET deleted_at = NOW() WHERE plan_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 50)
    private String region;

    @Column(name = "plan_image_url")
    private String planImageUrl;

    @Column(name = "is_share", columnDefinition = "tinyint(1) DEFAULT 0")
    private Boolean isShare;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
