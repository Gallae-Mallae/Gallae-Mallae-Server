package com.practice.OAuth2.domain.attraction.service;

import com.practice.OAuth2.domain.attraction.dto.PlaceFolderCreateRequest;
import com.practice.OAuth2.domain.attraction.dto.PlaceFolderInfoResponse;
import com.practice.OAuth2.domain.attraction.dto.PlaceFolderResponse;
import com.practice.OAuth2.domain.attraction.entity.Attraction;
import com.practice.OAuth2.domain.attraction.entity.ConnFolderPlace;
import com.practice.OAuth2.domain.attraction.entity.PlaceFolder;
import com.practice.OAuth2.domain.attraction.repository.AttractionRepository;
import com.practice.OAuth2.domain.attraction.repository.ConnFolderPlaceRepository;
import com.practice.OAuth2.domain.attraction.repository.PlaceFolderRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
import com.practice.OAuth2.global.exception.BadRequestException;
import com.practice.OAuth2.global.exception.DuplicateResourceException;
import com.practice.OAuth2.global.exception.ResourceNotFoundException;
import com.practice.OAuth2.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.DestroyFailedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceFolderService {

    private final PlaceFolderRepository placeFolderRepository;
    private final UserRepository userRepository;
    private final ConnFolderPlaceRepository connFolderPlaceRepository;
    private final AttractionRepository attractionRepository;

    @Transactional
    public void createFolder(UserPrincipal userPrincipal, PlaceFolderCreateRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = request.toEntity(user);

        placeFolderRepository.save(placeFolder);
    }

    @Transactional
    public void addAttractionInFolder(UserPrincipal userPrincipal, Long placeFolderId, Integer attractionId) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = placeFolderRepository.findById(placeFolderId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceFolder", "id", placeFolderId));

        if (!placeFolder.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("폴더를 생성한 사용자가 아닙니다.");
        }

        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attraction", "id", attractionId));

        ConnFolderPlace connFolderPlace = ConnFolderPlace.builder()
                .attraction(attraction)
                .placeFolder(placeFolder)
                .build();

        try {
            connFolderPlaceRepository.save(connFolderPlace);
        } catch (DataIntegrityViolationException e) {  // DB 제약조건 위반 시 발생하는 예외를 잡음
            throw new DuplicateResourceException("이미 폴더에 추가된 여행지입니다.");
        }
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

    @Transactional(readOnly = true)
    public List<PlaceFolderInfoResponse> getFolderInfo(UserPrincipal userPrincipal, Long placeFolderId) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = placeFolderRepository.findById(placeFolderId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceFolder", "id", placeFolderId));

        if (!placeFolder.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("폴더를 생성한 사용자가 아닙니다.");
        }

        // 중간 테이블 엔티티
        List<ConnFolderPlace> connFolderPlaceList = connFolderPlaceRepository.findAllByPlaceFolder(placeFolder);

        return connFolderPlaceList.stream()
                .map(PlaceFolderInfoResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateFolderName(UserPrincipal userPrincipal, Long placeFolderId, PlaceFolderCreateRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = placeFolderRepository.findById(placeFolderId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceFolder", "id", placeFolderId));

        if (!placeFolder.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("폴더를 생성한 사용자가 아닙니다.");
        }

        placeFolder.updateNameAndColor(request.getName(), request.getColor());
    }

    @Transactional
    public void deleteFolder(UserPrincipal userPrincipal, Long placeFolderId) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = placeFolderRepository.findById(placeFolderId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceFolder", "id", placeFolderId));

        if (!placeFolder.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("폴더를 생성한 사용자가 아닙니다.");
        }

        placeFolderRepository.delete(placeFolder); // cascade 로 중간테이블에 전파
    }

    @Transactional
    public void deleteAttractionInFolder(UserPrincipal userPrincipal, Long placeFolderId, Integer attractionId) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        PlaceFolder placeFolder = placeFolderRepository.findById(placeFolderId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaceFolder", "id", placeFolderId));

        if (!placeFolder.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("폴더를 생성한 사용자가 아닙니다.");
        }

        Attraction attraction = attractionRepository.findById(attractionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attraction", "id", attractionId));

        ConnFolderPlace connFolderPlace = connFolderPlaceRepository.findByPlaceFolderAndAttraction(placeFolder, attraction)
                .orElseThrow(() -> new ResourceNotFoundException("해당 폴더에 존재하지 않는 여행지입니다.", "attractionId", attractionId));

        connFolderPlaceRepository.delete(connFolderPlace);
    }
}
