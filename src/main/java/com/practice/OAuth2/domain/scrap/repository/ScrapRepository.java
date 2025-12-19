package com.practice.OAuth2.domain.scrap.repository;

import com.practice.OAuth2.domain.scrap.entity.Scrap;
import com.practice.OAuth2.domain.scrap.entity.ScrapFolder;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    // ScrapFolder로 스크랩 찾기
    List<Scrap> findByScrapFolder(ScrapFolder scrapFolder);

    // 특정 폴더의 스크랩 중 가장 최신(Top 1) 데이터 조회
    // ORDER BY created_at DESC LIMIT 1
    Optional<Scrap> findTopByScrapFolderOrderByCreatedAtDesc(ScrapFolder scrapFolder);
}
