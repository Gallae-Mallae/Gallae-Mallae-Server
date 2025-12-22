package com.practice.OAuth2.domain.plan.repository;

import com.practice.OAuth2.domain.plan.entity.Memo;
import java.util.List;

import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    // 특정 블록에 달린 메모 개수 세기
    // 용도: 새 메모 생성 시 마지막 순서(orderIndex)를 구하기 위함
    Integer countByScheduleBlock(ScheduleBlock scheduleBlock);

    // 특정 블록의 모든 메모 조회 (순서대로 정렬)
    // 용도: 순서 변경(Reorder) 로직이나, 화면에 뿌려줄 때 사용
    List<Memo> findAllByScheduleBlockOrderByOrderIndexAsc(ScheduleBlock scheduleBlock);
}
