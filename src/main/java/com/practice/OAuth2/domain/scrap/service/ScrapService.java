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

    // 스크랩 폴더 생성
    public Long createScrapFolder(Long userId, ScrapReqest.CreateScrapFolder req){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 x"));
        ScrapFolder folder = ScrapFolder.builder()
                .user(user)
                .name(req.getName())
                .folderImageUrl(req.getFolderImageUrl())
                .build();

        return scrapFolderRepository.save(folder).getFolderId();
    }

    // 스크랩 폴더 조회
    public List<ScrapFolderResponse> getScrapFolders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 x"));

        List<ScrapFolder> folders = scrapFolderRepository.findByUser(user);

        // 각 폴더별로 최신 스크랩 이미지를 조회하여 DTO 생성
        return folders.stream().map(folder -> {
            // 해당 폴더의 가장 최신 스크랩 1개 조회
            Scrap latestScrap = scrapRepository.findTopByScrapFolderOrderByCreatedAtDesc(folder)
                    .orElse(null);

            String thumbnailParams = null;
            if (latestScrap != null) {
                // 스크랩에 저장된 이미지 URL 가져오기
                thumbnailParams = latestScrap.getImageUrl();
            }

            // DTO 생성자 호출 (폴더 정보 + 최신 이미지 URL)
            return new ScrapFolderResponse(folder, thumbnailParams);
        }).collect(Collectors.toList());
    }

    // 스크랩 폴더 수정
    public void updateScrapFolder(Long userId, Long folderId, ScrapReqest.UpdateScrapFolder req) {
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더가 없습니다."));

        validateFolderOwnership(folder, userId);

        folder.updateScrapFolder(req.getName());
    }

    // 스크랩 폴더 삭제
    public void deleteScrapFolder(Long userId, Long folderId) {
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더가 없습니다."));

        validateFolderOwnership(folder, userId);

        // Soft Delete (@SQLDelete) 작동(자동으로)
        scrapFolderRepository.delete(folder);
    }

    //--------------------------------------------------------------------------------------------
    // 스크랩 생성
    public Long createScrap(Long userId, Long folderId, ScrapReqest.CreateScrap req){
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더 x"));

        validateFolderOwnership(folder, userId);

        Scrap scrap = Scrap.builder()
                .scrapFolder(folder)
                .title(req.getTitle())
                .content(req.getContent())
                .description(req.getDescription())
                .originalLink(req.getOriginalLink())
                .imageUrl(req.getImageUrl())
                .build();
        return scrapRepository.save(scrap).getScrapId();
    }

    // 스크랩 조회
    public List<ScrapResponse> getScraps(Long userId, Long folderId){
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더 없음"));

        validateFolderOwnership(folder, userId);

        return scrapRepository.findByScrapFolder(folder).stream()
                .map(ScrapResponse::from)
                .collect(Collectors.toList());
    }

    // 스크랩 수정
    public void updateScrap(Long userId, Long scrapId, ScrapReqest.UpdateScrap req) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new IllegalArgumentException("스크랩이 없습니다."));

        // 권한 확인
        validateScrapOwnership(scrap, userId);

        scrap.updateScrap(req.getTitle(), req.getContent(), req.getDescription(), req.getOriginalLink(), req.getImageUrl());
    }

    // 스크랩 삭제
    public void deleteScrap(Long userId, Long scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new IllegalArgumentException("스크랩이 없습니다."));

        validateScrapOwnership(scrap, userId);

        scrapRepository.delete(scrap);
    }

    //-------------------------------------------------------------------------------------------------
    // 폴더의 주인이 현재 로그인한 유저인지 확인
    private void validateFolderOwnership(ScrapFolder folder, Long userId) {
        if (!folder.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 폴더에 대한 권한이 없습니다.");
        }
    }

    // 스크랩 -> 폴더 -> 유저 : 로 확인
    private void validateScrapOwnership(Scrap scrap, Long userId) {
        if (!scrap.getScrapFolder().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 스크랩에 대한 권한이 없습니다.");
        }
    }
}
