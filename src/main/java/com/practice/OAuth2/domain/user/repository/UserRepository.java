package com.practice.OAuth2.domain.user.repository;

import com.practice.OAuth2.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 시 필요
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    // 닉네임 중복 체크 등에 사용
    Boolean existsByNickname(String nickname);

    @Transactional
    @Modifying
    @Query(value = "UPDATE users SET deleted_at = NULL WHERE email = :email", nativeQuery = true)
    int restoreUser(String email);

}
