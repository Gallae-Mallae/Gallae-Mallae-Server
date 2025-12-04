package com.practice.OAuth2.domain.scrap.repository;

import com.practice.OAuth2.domain.scrap.entity.ScrapFolder;
import com.practice.OAuth2.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapFolderRepository extends JpaRepository<ScrapFolder, Long> {
    // user 통해서 폴더 찾기
    List<ScrapFolder> findByUser(User user);
}
