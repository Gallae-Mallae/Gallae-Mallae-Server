package com.practice.OAuth2.domain.auth.repository;

import com.practice.OAuth2.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
// JPA가 아닌 CrudRepository를 상속받지만 사용법(save, findById)은 동일
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}