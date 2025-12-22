package com.practice.OAuth2.domain.attraction.repository;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttractionRepository extends JpaRepository<Attraction, Integer> {

    // 키워드 검색
    List<Attraction> findByTitleContaining(String keyword);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Attraction a SET a.likeCount = a.likeCount + 1 WHERE a.attrId = :attrId")
    void incrementLikeCount(@org.springframework.data.repository.query.Param("attrId") Integer attrId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Attraction a SET a.likeCount = a.likeCount - 1 WHERE a.attrId = :attrId")
    void decrementLikeCount(@org.springframework.data.repository.query.Param("attrId") Integer attrId);
}
