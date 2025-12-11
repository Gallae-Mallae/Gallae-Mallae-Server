package com.practice.OAuth2.domain.plan.repository;

import com.practice.OAuth2.domain.plan.entity.Memo;
import com.practice.OAuth2.domain.plan.entity.Plan;
import com.practice.OAuth2.domain.plan.entity.PlanMember;
import com.practice.OAuth2.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanMemberRepository extends JpaRepository<PlanMember, Long> {

    // 1. 특정 여행의 현재 참여 중인 멤버 목록 조회 (나간 사람 제외)
    // 용도: 채팅방 참여자 목록 표시, Redis 캐싱용
    // @EntityGraph는 User 정보를 한 번에 가져와서 N+1 문제를 방지함
    @EntityGraph(attributePaths = {"user"})
    List<PlanMember> findByPlan_PlanIdAndLeftAtIsNull(Long planId);

    // 2. 내가 참여 중인 여행 목록 조회
    @EntityGraph(attributePaths = {"plan"})
    List<PlanMember> findByUser_UserIdAndLeftAtIsNull(Long userId);

    // 3. 특정 유저가 이 방에 들어온 적이 있는지 확인 (재입장 로직용)
    // leftAt 상관없이 기록 자체를 찾음
    Optional<PlanMember> findByPlanAndUser(Plan plan, User user);

    // 4.STOMP 인터셉터 권한 체크용
    boolean existsByPlan_PlanIdAndUser_UserIdAndLeftAtIsNull(Long planId, Long userId);

    // 5. 방의 인원수 체크 (Redis가 없을 때 백업용)
    long countByPlan_PlanIdAndLeftAtIsNull(Long planId);
}
