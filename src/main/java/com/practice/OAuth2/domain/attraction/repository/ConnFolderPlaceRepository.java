package com.practice.OAuth2.domain.attraction.repository;

import com.practice.OAuth2.domain.attraction.entity.ConnFolderPlace;
import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnFolderPlaceRepository extends JpaRepository<ConnFolderPlace, Long> {

    // N + 1 페치조인
    @Query("select c from ConnFolderPlace c join fetch c.attraction where c.placeFolder = :placeFolder")
    List<ConnFolderPlace> findAllByPlaceFolder(@Param("placeFolder") PlaceFolder placeFolder);
}
