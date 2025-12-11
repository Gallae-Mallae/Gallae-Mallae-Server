package com.practice.OAuth2.domain.plan.repository;

import com.practice.OAuth2.domain.plan.entity.ScheduleBlock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {
    List<ScheduleBlock> findAllByPlan_PlanIdOrderByDayAscStartTimeAsc(Long planId);
}
