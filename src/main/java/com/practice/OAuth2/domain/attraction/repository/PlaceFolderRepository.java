package com.practice.OAuth2.domain.attraction.repository;

import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceFolderRepository extends JpaRepository<PlaceFolder, Long> {
}
