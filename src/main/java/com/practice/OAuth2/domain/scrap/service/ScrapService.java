package com.practice.OAuth2.domain.scrap.service;
import com.practice.OAuth2.domain.scrap.dto.ScrapFolderResponse;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest;
import com.practice.OAuth2.domain.scrap.dto.ScrapReqest.CreateScrap;
import com.practice.OAuth2.domain.scrap.dto.ScrapResponse;
import com.practice.OAuth2.domain.scrap.entity.Scrap;
import com.practice.OAuth2.domain.scrap.entity.ScrapFolder;
import com.practice.OAuth2.domain.scrap.repository.ScrapFolderRepository;
import com.practice.OAuth2.domain.scrap.repository.ScrapRepository;
import com.practice.OAuth2.domain.user.entity.User;
import com.practice.OAuth2.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // scrap folder 생성
    @Transactional
    public Long createScrapFolder(Long userId, ScrapReqest.CreateScrapFolder req){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 x"));
        ScrapFolder folder = ScrapFolder.builder()
                //.user(user)
                .name(req.getName())
                .description(req.getDescription())
                .build();
        return scrapFolderRepository.save(folder).getFolderId();
    }

    // scrap 생성
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

    // scrap folder 조회
    public List<ScrapFolderResponse> getScrapFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 x"));

        // 유저가 만든 폴더들 다 찾아오기
        return scrapFolderRepository.findByUser(user).stream()
                .map(ScrapFolderResponse::from)
                .collect(Collectors.toList());
    }

    // scrap 조회
    @Transactional
    public List<ScrapResponse> getScraps(Long folderId){
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더 없음"));

        return scrapRepository.findByScrapFolder(folder).stream()
                .map(ScrapResponse::from)
                .collect(Collectors.toList());
    }

    // 폴더 수정
    @Transactional
    public void updateScrapFolder(Long userId, Long folderId, ScrapReqest.UpdateScrapFolder req) {
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더가 없습니다."));

        // user 통해서 폴더 주인 확인로직 추가 필요

        folder.updateScrapFolder(req.getName(), req.getDescription());
    }

    // 폴더 삭제
    @Transactional
    public void deleteScrapFolder(Long userId, Long folderId) {
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더가 없습니다."));

        // 권한 체크

        // Soft Delete (@SQLDelete) 작동(자동으로)
        scrapFolderRepository.delete(folder);
    }

    // 스크랩 수정
    @Transactional
    public void updateScrap(Long userId, Long scrapId, ScrapReqest.UpdateScrap req) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new IllegalArgumentException("스크랩이 없습니다."));

        // 권한 확인

        scrap.updateScrap(req.getTitle(), req.getContent(), req.getOriginalLink(), req.getImageUrl());
    }

    // 스크랩 삭제
    @Transactional
    public void deleteScrap(Long userId, Long scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new IllegalArgumentException("스크랩이 없습니다."));

        // User 통해서 폴더 주인 확인로직 추가 필요

        scrapRepository.delete(scrap);
    }
}
