package com.practice.OAuth2.domain.scrap.repository;

import com.practice.OAuth2.domain.scrap.entity.Scrap;
import com.practice.OAuth2.domain.scrap.entity.ScrapFolder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    // ScrapFolder로 스크랩 찾기
    List<Scrap> findByScrapFolder(ScrapFolder scrapFolder);
}
