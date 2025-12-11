package com.practice.OAuth2.domain.plan.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "plans")
@SQLDelete(sql = "UPDATE plans SET deleted_at = NOW() WHERE plan_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Plan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "plan_image_url")
    private String planImageUrl;

    // 추가: STOMP 방 입장/초대용 코드 (랜덤 UUID)
    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Plan(String title, LocalDate startDate, LocalDate endDate, String region, String planImageUrl) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.planImageUrl = planImageUrl;
        // 생성 시 초대코드 자동 발급
        this.inviteCode = UUID.randomUUID().toString();
    }
}
