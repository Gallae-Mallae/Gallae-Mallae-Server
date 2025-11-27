package com.practice.OAuth2.repository;

import com.practice.OAuth2.model.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
// JPA가 아닌 CrudRepository를 상속받지만 사용법(save, findById)은 동일합니다.
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}