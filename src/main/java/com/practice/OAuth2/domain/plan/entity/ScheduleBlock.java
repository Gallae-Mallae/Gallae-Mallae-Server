package com.practice.OAuth2.domain.plan.entity;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
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
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "schedule_blocks")
@SQLDelete(sql = "UPDATE schedule_blocks SET deleted_at = NOW() WHERE block_id = ?")
@Where(clause = "deleted_at IS NULL")
public class ScheduleBlock extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_id")
    private Attraction attraction;

    private Integer day;

    @Column(columnDefinition = "int DEFAULT 0")
    private Integer sequence;

    @Column(length = 100)
    private String title;

    @Column(name = "simple_memo", columnDefinition = "TEXT")
    private String simpleMemo;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
