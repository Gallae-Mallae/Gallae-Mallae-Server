package com.practice.OAuth2.domain.plan.repository;

import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    // 특정 planId에 속한 모든 스케줄 블록을 DB에서 긁어오기
    // 가져올 때 1일 차 → 2일 차 순서로, 같은 날짜 내에서는 아침 → 저녁 순서로 정렬해서 가져오기
    // 엔티티그래프 설정 : 스케줄 블록 가져올 때 attraction 정보도 미리 조인(JOIN FETCH)해서 한 방에 가져와
    @EntityGraph(attributePaths = {"attraction"})
    List<ScheduleBlock> findAllByPlan_PlanIdOrderByDayAscStartTimeAsc(Long planId);

    // 특정 day의 스케줄 조회 (시간순 정렬)
    @EntityGraph(attributePaths = {"attraction"})
    List<ScheduleBlock> findAllByPlan_PlanIdAndDayOrderByStartTimeAsc(Long planId, Integer day);
}
