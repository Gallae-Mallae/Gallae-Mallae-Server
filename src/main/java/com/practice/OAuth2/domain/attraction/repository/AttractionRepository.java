package com.practice.OAuth2.domain.attraction.repository;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttractionRepository extends JpaRepository<Attraction, Integer> {

    // 키워드 검색
    List<Attraction> findByTitleContaining(String keyword);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Attraction a SET a.likeCount = a.likeCount + 1 WHERE a.attrId = :attrId")
    void incrementLikeCount(@org.springframework.data.repository.query.Param("attrId") Integer attrId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Attraction a SET a.likeCount = a.likeCount - 1 WHERE a.attrId = :attrId")
    void decrementLikeCount(@org.springframework.data.repository.query.Param("attrId") Integer attrId);


    @Query(
            value = "SELECT a FROM Attraction a " +
                    "WHERE a.contentTypeId = 12 " +
                    "OR a.contentTypeId = 25 " +
                    "OR (a.overview IS NOT NULL AND LENGTH(a.overview) >= 15)",
            countQuery = "SELECT count(a) FROM Attraction a " +
                    "WHERE a.contentTypeId = 12 " +
                    "OR a.contentTypeId = 25 " +
                    "OR (a.overview IS NOT NULL AND LENGTH(a.overview) >= 15)"
    )
    Page<Attraction> findRagData(Pageable pageable);

    // 복합 조건 검색 (JOIN FETCH로 N+1 해결, countQuery 분리)
    @Query(value = "SELECT a FROM Attraction a " +
            "JOIN FETCH a.sido " +
            "JOIN FETCH a.gugun g " +
            "JOIN FETCH g.sido " +
            "WHERE a.createdAt >= :date " +
            "AND a.contentTypeId = :contentTypeId " +
            "AND a.likeCount >= :minLikeCount " +
            "ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(a) FROM Attraction a " +
            "WHERE a.createdAt >= :date " +
            "AND a.contentTypeId = :contentTypeId " +
            "AND a.likeCount >= :minLikeCount")
    Page<Attraction> findByComplexCondition(
            @Param("date") LocalDateTime date,
            @Param("contentTypeId") Integer contentTypeId,
            @Param("minLikeCount") Long minLikeCount,
            Pageable pageable);
}
