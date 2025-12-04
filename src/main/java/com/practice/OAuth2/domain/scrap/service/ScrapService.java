package com.practice.OAuth2.domain.scrap.service;

import com.practice.OAuth2.domain.scrap.dto.ScrapReqest;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest.CreateScrap;
import com.practice.OAuth2.domain.scrap.dto.ScrapResponse;
import com.practice.OAuth2.domain.scrap.entity.Scrap;
import com.practice.OAuth2.domain.scrap.entity.ScrapFolder;
import com.practice.OAuth2.domain.scrap.repository.ScrapFolderRepository;
import com.practice.OAuth2.domain.scrap.repository.ScrapRepository;
import com.practice.OAuth2.domain.user.entity.User;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ScrapService {

    private final ScrapFolderRepository scrapFolderRepository;
    private final ScrapRepository scrapRepository;
    // userrepository 나중에 가져오기

    // 폴더 생성
    @Transactional
    public Long createScrapFolder(Long userId, ScrapReqest.CreateScrapFolder req){
        //User user = userRepository.findById(userId)
        //        .orElseThrow(() -> new IllegalArgumentException("유저 x"));
        ScrapFolder folder = ScrapFolder.builder()
                //.user(user)
                .name(req.getName())
                .description(req.getDescription())
                .build();
        return scrapFolderRepository.save(folder).getFolderId();
    }

    // 스크랩 생성
    @Transactional
    public Long createScrap(Long userId, Long folderId, ScrapReqest.CreateScrap req){
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더 x"));

        // 내 폴더인지 확인 필요

        Scrap scrap = Scrap.builder()
                .scrapFolder(folder)
                .title(req.getTitle())
                .content(req.getContent())
                .originalLink(req.getOriginLink())
                .imageUrl(req.getImageUrl())
                .build();
        return scrapRepository.save(scrap).getScrapId();
    }

    // 조회
    public List<ScrapResponse> getScraps(Long folderId){
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더 없음"));

        return scrapRepository.findByScrapFolder(folder).stream()
                .map(ScrapResponse::from)
                .collect(Collectors.toList());
    }
}
