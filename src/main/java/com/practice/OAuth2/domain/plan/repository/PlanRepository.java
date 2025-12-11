package com.practice.OAuth2.domain.plan.repository;

import com.practice.OAuth2.domain.plan.entity.Plan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    // 초대 코드로 여행 계획 조회 (친구 초대 링크 클릭 시 사용)
    Optional<Plan> findByInviteCode(String inviteCode);
}
