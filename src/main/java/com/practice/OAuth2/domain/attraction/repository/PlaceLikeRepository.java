package com.practice.OAuth2.domain.attraction.repository;

import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.entity.PlaceLike;
import com.practice.OAuth2.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceLikeRepository extends JpaRepository<PlaceLike, Long> {
    Optional<PlaceLike> findByUserAndAttraction(User user, Attraction attraction);
    boolean existsByUserAndAttraction(User user, Attraction attraction);
    List<PlaceLike> findAllByUser(User user);
}
