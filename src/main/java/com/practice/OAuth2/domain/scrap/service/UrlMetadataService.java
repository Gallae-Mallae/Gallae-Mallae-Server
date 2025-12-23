package com.practice.OAuth2.domain.scrap.service;

import com.practice.OAuth2.domain.scrap.dto.LinkMetadataResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
public class UrlMetadataService {

    public LinkMetadataResponse extractMetadata(String url) {
        // 유튜브 인지 확인
        if (isYouTubeUrl(url)) {
            String videoId = extractYouTubeVideoId(url);
            if (videoId != null) {
                // 유튜브 공식 썸네일 주소 조합
                String imageUrl = "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
                String title = "YouTube Video"; // 유튜브는 접속 안 하면 제목을 알 수 없으므로 기본값
                return new LinkMetadataResponse(title, url, imageUrl, "YouTube Video");
            }
        }
        try {
            // 1. 해당 URL의 HTML 문서를 가져옴 (타임아웃 5초 설정)
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36") // 브라우저인 척 위장
                    .referrer("http://www.google.com")
                    .timeout(5000)
                    .get();

            // 네이버 전용 (iframe)
            if (url.contains("blog.naver.com")) {
                Element iframe = doc.select("iframe#mainFrame").first();
                if (iframe != null) {
                    String realUrl = "https://blog.naver.com" + iframe.attr("src");
                    // 진짜 주소로 문서를 교체 (덮어쓰기)
                    doc = Jsoup.connect(realUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .referrer("http://www.google.com")
                            .timeout(5000)
                            .get();
                }
            }

            // 2. Open Graph 태그 파싱
            String title = getMetaTagContent(doc, "og:title", "twitter:title", "title");
            String image = getMetaTagContent(doc, "og:image", "twitter:image");
            String description = getMetaTagContent(doc, "og:description", "twitter:description", "description");

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
            return new LinkMetadataResponse("제목 없음", url, "https://gm-public-bucket.s3.ap-northeast-2.amazonaws.com/scrapdefault.png", "정보를 가져올 수 없습니다.");
        }
    }

    // 메타 태그 내용 추출 헬퍼 메서드
    private String getMetaTagContent(Document doc, String... keys) {
        for (String key : keys) {
            // 1. property="key" 검색 (예: <meta property="og:image">)
            Element element = doc.select("meta[property=" + key + "]").first();
            if (element != null) return element.attr("content");

            // 2. name="key" 검색 (예: <meta name="twitter:image">)
            element = doc.select("meta[name=" + key + "]").first();
            if (element != null) return element.attr("content");
        }
        return null;
    }

    // 유튜브 URL 패턴 정규식
    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    private String extractYouTubeVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
