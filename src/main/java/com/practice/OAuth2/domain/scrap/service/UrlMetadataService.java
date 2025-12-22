package com.practice.OAuth2.domain.scrap.service;

import com.practice.OAuth2.domain.scrap.dto.LinkMetadataResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UrlMetadataService {

    public LinkMetadataResponse extractMetadata(String url) {
        try {
            // 1. 해당 URL의 HTML 문서를 가져옴 (타임아웃 5초 설정)
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36") // 브라우저인 척 위장
                    .timeout(5000)
                    .get();

            // 2. Open Graph 태그 파싱
            String title = getMetaTagContent(doc, "og:title");
            String image = getMetaTagContent(doc, "og:image");
            String description = getMetaTagContent(doc, "og:description");

            // 3. 만약 og 태그가 없으면 일반 title, img 태그에서 시도
            if (title == null || title.isEmpty()) {
                title = doc.title();
            }
            if (image == null || image.isEmpty()) {
                // 대표 이미지가 없으면 첫 번째 이미지 태그라도 가져오기 (선택사항)
                Element firstImg = doc.select("img").first();
                if (firstImg != null) {
                    image = firstImg.attr("abs:src");
                }
            }

            return new LinkMetadataResponse(title, url, image, description);

        } catch (IOException e) {
            // ("URL 파싱 실패: {}", url, e);
            // 실패 시 기본값 혹은 예외 처리
            return new LinkMetadataResponse("제목 없음", url, "[https://default-image.url](https://default-image.url)", "정보를 가져올 수 없습니다.");
        }
    }

    // 메타 태그 내용 추출 헬퍼 메서드
    private String getMetaTagContent(Document doc, String property) {
        Element element = doc.select("meta[property=" + property + "]").first();
        if (element != null) {
            return element.attr("content");
        }
        return null;
    }
}
