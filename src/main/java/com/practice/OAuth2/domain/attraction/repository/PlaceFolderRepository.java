package com.practice.OAuth2.domain.attraction.repository;

import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import com.practice.OAuth2.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceFolderRepository extends JpaRepository<PlaceFolder, Long> {

    List<PlaceFolder> findAllByUser(User user);

    // 페치 조인으로 N + 1 방지
    // connFolderPlaces 에 등록 안된 PlaceFolder 도 가져와야하니 left join 필수
    @Query("select p from PlaceFolder p left join fetch p.connFolderPlaces where p.user = :user")
    List<PlaceFolder> findAllByUserWithConnFolderPlaces(@Param("user") User user);

}
