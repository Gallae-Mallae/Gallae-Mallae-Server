package com.practice.OAuth2.domain.plan.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "memos")
@SQLDelete(sql = "UPDATE memos SET deleted_at = NOW() WHERE memo_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Memo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long memoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private ScheduleBlock scheduleBlock;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(length = 20)
    private String type;

    public void update(String content, String linkUrl, String type) {
        this.content = content;
        this.linkUrl = linkUrl;
        this.type = type;
    }
}
