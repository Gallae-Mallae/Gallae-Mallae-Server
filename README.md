# ✈️ 갈래말래 (Gallae-Mallae) Server

<div align="center">

<img src="assets/images/logo.png" width="300"/>

### **"AI 추천부터 실시간 일정 조율까지, 함께 그리는 완벽한 여행"**

지도 탐색, AI 맞춤 추천, 그리고 실시간 협업을 통해 친구들과 쉽고 빠르게 여행 계획을 세울 수 있는 웹 서비스입니다.

</div>

---

## 🌟 프로젝트 소개

여행을 계획할 때마다 쏟아지는 정보와 친구들 간의 의견 조율로 지치신 적 없으신가요? **갈래말래**는 여행 준비 과정의 피로도를 낮추고 즐거움을 극대화하기 위해 기획되었습니다.

* **🤖 AI 맞춤 여행지 추천:** Pinecone 벡터 DB와 RAG(Retrieval-Augmented Generation) 기반 챗봇을 통해 내 취향에 딱 맞는 여행지와 코스를 제안받을 수 있습니다.
* **🤝 실시간 협업 플래너:** WebSocket(STOMP)을 통해 초대 코드 하나로 친구들을 모으고, 실시간으로 일정 블록을 이동하며 함께 시간표를 완성합니다.
* **🗺️ 대규모 장소 탐색:** Geohashing 기반 공간 인덱싱 및 클러스터링을 통해 전국 약 25만 개의 장소 데이터를 지연 없이 탐색하세요.
* **📁 스마트 스크랩:** Jsoup을 활용한 메타데이터 크롤링으로 링크만 붙여넣어도 미리보기가 포함된 스크랩북을 만들 수 있습니다.

---

## 📅 프로젝트 기간
* **2024.01 ~ 2024.02** (약 5주)

---

## 👥 팀원 소개

<div align="center">
  <table width="100%">
    <tr>
      <td align="center">
        <a href="https://github.com/SHSong99">
          <img src="https://github.com/SHSong99.png" width="100px;" alt="송성현 프로필"/><br />
          <sub><b>송성현</b></sub>
        </a><br />
        Infra & DevOps
      </td>
      <td align="center">
        <a href="https://github.com/theundergroundt">
          <img src="https://github.com/theundergroundt.png" width="100px;" alt="김상지 프로필"/><br />
          <sub><b>김상지</b></sub>
        </a><br />
        Backend (Auth/Plan)
      </td>
      <td align="center">
        <a href="https://github.com/DooDooLee">
          <img src="https://github.com/DooDooLee.png" width="100px;" alt="이승엽 프로필"/><br />
          <sub><b>이승엽</b></sub>
        </a><br />
        Backend (Search/AI)
      </td>
      <td align="center">
        <a href="https://github.com/zwongraphic">
          <img src="https://github.com/zwongraphic.png" width="100px;" alt="이지원 프로필"/><br />
          <sub><b>이지원</b></sub>
        </a><br />
        Frontend
      </td>
    </tr>
  </table>
</div>

---

## 🛠️ 기술 스택

### 💻 Backend
* **Language:** Java 17
* **Framework:** Spring Boot 3.0.5
* **Security:** Spring Security, OAuth2 Client, JWT (jjwt)
* **Data:** Spring Data JPA, MyBatis (For Complex Search), MySQL 8.0
* **Caching & Concurrency:** Redis (Redisson)
* **Real-time:** WebSocket (STOMP), SockJS
* **AI:** Pinecone (Vector DB), RAG implementation
* **External:** AWS S3 (Spring Cloud AWS), Jsoup (Metadata Scraping)

### 🎨 Frontend
* **Core:** React, Vite, TypeScript
* **State Management:** Zustand
* **Map:** Kakao Maps API
* **Communication:** Axios, STOMP.js

### ⚙️ DevOps
* **CI/CD:** GitHub Actions, Docker, AWS EC2, S3, CodeDeploy

---

## 🏗️ 시스템 아키텍처
<img src="assets/images/architecture.png" width="100%"/>

---

## 📂 프로젝트 구조
```text
src/main/java/com/practice/OAuth2/
├── domain/
│   ├── ai/          # RAG 기반 AI 추천 서비스 (Pinecone 연동)
│   ├── attraction/  # 관광지 검색, 클러스터링, 좋아요 및 폴더 관리
│   ├── auth/        # JWT 기반 인증, 토큰 재발급, 로그아웃
│   ├── plan/        # 여행 계획 및 실시간 협업 스케줄링 (WebSocket)
│   ├── scrap/       # 외부 링크/텍스트 스크랩 및 메타데이터 추출
│   └── user/        # 사용자 프로필 관리 및 권한
├── global/
│   ├── common/      # 공통 Response 및 BaseEntity
│   ├── config/      # Redis, Security, WebSocket, S3 설정
│   ├── exception/   # 전역 예외 처리 (GlobalExceptionHandler)
│   ├── security/    # JWT 필터 및 UserPrincipal
│   └── util/        # Cookie, Token 관리 유틸리티
└── resources/
    └── mapper/      # MyBatis XML 매퍼 (관광지 검색 최적화)
```

---

## 📌 주요 API 엔드포인트

| 도메인 | 엔드포인트 | 설명 |
| --- | --- | --- |
| **Auth** | `POST /api/auth/reissue` | JWT 액세스 토큰 재발급 |
| **Plan** | `POST /api/plans` | 신규 여행 계획 생성 |
| **Plan** | `POST /api/plans/join` | 초대 코드로 여행 계획 참여 |
| **Schedule** | `PATCH /api/schedules/{id}/position` | 일정 블록 이동 (실시간 동기화) |
| **Attraction**| `GET /api/attractions/map` | 지도 마커 및 클러스터 데이터 조회 |
| **AI** | `GET /api/ai/chat` | AI 챗봇 맞춤 추천 대화 |
| **Scrap** | `POST /api/scraps` | 외부 링크 스크랩 및 메타데이터 저장 |

---

## ⚙️ 시작 가이드

### 환경 변수 설정 (`application.yml`)
`src/main/resources/application.yml` 파일을 생성하고 아래 형식을 참조하여 작성하세요.

```yaml
spring:
  datasource:
    url: jdbc:mysql://{HOST}:3306/gallae_mallae
    username: {USERNAME}
    password: {PASSWORD}
  redis:
    host: {REDIS_HOST}
    port: 6379
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: {KAKAO_CLIENT_ID}
            client-secret: {KAKAO_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"

cloud:
  aws:
    credentials:
      access-key: {AWS_ACCESS_KEY}
      secret-key: {AWS_SECRET_KEY}
    s3:
      bucket: {BUCKET_NAME}

app:
  auth:
    tokenSecret: {JWT_SECRET}
    tokenExpirationMsec: 1800000 # 30 mins
  oauth2:
    authorizedRedirectUris:
      - http://localhost:3000/oauth2/redirect
```

### 실행 방법
```bash
# 빌드
./gradlew build

# 실행
java -jar build/libs/OAuth2-0.0.1-SNAPSHOT.jar
```

---

## 📊 ERD (Entity Relationship Diagram)
<img src="assets/images/erd.png" width="100%"/>
