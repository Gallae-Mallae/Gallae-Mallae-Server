package com.practice.OAuth2.domain.attraction.service;

import com.practice.OAuth2.domain.attraction.dto.PlaceFolderCreateRequest;
import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import com.practice.OAuth2.domain.attraction.repository.PlaceFolderRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.exception.ResourceNotFoundException;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceFolderService {

    private final PlaceFolderRepository placeFolderRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createFolder(UserPrincipal userPrincipal, PlaceFolderCreateRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = request.toEntity(user);

        placeFolderRepository.save(placeFolder);
    }
}
