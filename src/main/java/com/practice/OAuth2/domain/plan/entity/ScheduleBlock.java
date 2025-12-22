package com.practice.OAuth2.domain.plan.entity;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.global.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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
    @JoinColumn(name = "attr_id", nullable = true)
    private Attraction attraction;

    @Column(name = "day")
    private Integer day;

    @Column(length = 100)
    private String title;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 양방향 매핑 추가
    // 블록 조회할때 메모도 같이 조회
    @OneToMany(mappedBy = "scheduleBlock", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Memo> memos = new ArrayList<>();

    // 블록 옮기기
    public void changePosition(Integer day, LocalTime startTime) {

        Duration duration = Duration.between(this.startTime, this.endTime);

        this.day = day;
        this.startTime = startTime;

        this.endTime = startTime.plus(duration);
    }

    // 블록 시간 조정하기
    public void updateEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
