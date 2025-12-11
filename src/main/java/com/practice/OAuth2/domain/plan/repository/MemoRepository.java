package com.practice.OAuth2.domain.plan.repository;

import com.practice.OAuth2.domain.plan.entity.Memo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    // 스케줄 블럭에 달린 메모 조회
    List<Memo> findAllByScheduleBlock_BlockId(Long blockId);
}
