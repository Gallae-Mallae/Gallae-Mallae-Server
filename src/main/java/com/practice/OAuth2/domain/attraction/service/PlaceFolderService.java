package com.practice.OAuth2.domain.attraction.service;

import com.practice.OAuth2.domain.attraction.dto.PlaceFolderCreateRequest;
import com.practice.OAuth2.domain.attraction.dto.PlaceFolderResponse;
import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import com.practice.OAuth2.domain.attraction.repository.PlaceFolderRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.exception.ResourceNotFoundException;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<PlaceFolderResponse> getFolderList(UserPrincipal userPrincipal) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        List<PlaceFolder> folders = placeFolderRepository.findAllByUserWithConnFolderPlaces(user);

        return folders.stream()
                .map(PlaceFolderResponse::from)
                .collect(Collectors.toList());
    }
}
