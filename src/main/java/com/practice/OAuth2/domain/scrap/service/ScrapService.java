package com.practice.OAuth2.domain.scrap.service;
import com.practice.OAuth2.domain.scrap.dto.LinkMetadataResponse;
import com.practice.OAuth2.domain.scrap.dto.ScrapFolderResponse;
import com.practice.OAuth2.domain.scrap.dto.ScrapRequest;
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
    private final UrlMetadataService urlMetadataService;

    // 스크랩 폴더 생성
    public Long createScrapFolder(Long userId, ScrapRequest.CreateScrapFolder req){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 x"));
        ScrapFolder folder = ScrapFolder.builder()
                .user(user)
                .name(req.getName())
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

            // 스크랩 개수
            int count = scrapRepository.countByScrapFolder(folder);

            // DTO 생성자 호출 (폴더 정보 + 최신 이미지 URL)
            return new ScrapFolderResponse(folder, thumbnailParams, count);
        }).collect(Collectors.toList());
    }

    // 스크랩 폴더 수정
    public void updateScrapFolder(Long userId, Long folderId, ScrapRequest.UpdateScrapFolder req) {
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
    // 스크랩 생성(+이미지 누락 시 자동 채움 기능)
    public Long createScrap(Long userId, Long folderId, ScrapRequest.CreateScrap req){
        ScrapFolder folder = scrapFolderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("폴더 x"));

        validateFolderOwnership(folder, userId);

        String imageUrl = req.getImageUrl();
        String content = req.getContent(); // OGP 설명

        if ((imageUrl == null || imageUrl.isEmpty()) && req.getOriginalLink() != null) {
            try {
                LinkMetadataResponse metadata = urlMetadataService.extractMetadata(req.getOriginalLink());
                imageUrl = metadata.getImageUrl();
                content = metadata.getDescription(); // 설명도 없으면 채워줌
            } catch (Exception e) {
                // 스크래핑 실패해도 스크랩 생성은 되어야 한다
                // log.warn("Auto-scraping failed: {}", e.getMessage());
            }
        }

        Scrap scrap = Scrap.builder()
                .scrapFolder(folder)
                .title(req.getTitle())
                .description(req.getDescription())
                .originalLink(req.getOriginalLink())
                .imageUrl(imageUrl)
                .content(content)
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

    // 스크랩 수정 + 링크 변경시 자동 스크래핑
    public void updateScrap(Long userId, Long scrapId, ScrapRequest.UpdateScrap req) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new IllegalArgumentException("스크랩이 없습니다."));

        // 권한 확인
        validateScrapOwnership(scrap, userId);

        String newLink = req.getOriginalLink();
        String newImageUrl = scrap.getImageUrl(); // 기본값: 기존 유지
        String newContent = scrap.getContent();   // 기본값: 기존 OGP설명 유지

        // 링크가 존재 + 기존과 다르다면 -> 다시 긁어오기
        if (newLink != null && !newLink.equals(scrap.getOriginalLink())) {
            // 새 주소로 메타데이터 긁어오기
            LinkMetadataResponse metadata = urlMetadataService.extractMetadata(newLink);

            // 이미지와 OGP설명(content) 교체
            newImageUrl = metadata.getImageUrl();
            newContent = metadata.getDescription(); // OGP 설명을 content 필드에 저장
        }

        scrap.updateScrap(req.getTitle(), req.getDescription(), newLink, newImageUrl, newContent);
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
