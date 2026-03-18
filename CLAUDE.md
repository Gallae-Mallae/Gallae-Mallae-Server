# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Gallae-Mallae (갈래말래) — a collaborative travel planning backend built with Spring Boot 3.0.5 and Java 17. Features real-time WebSocket collaboration, OAuth2 (Kakao) authentication, AI-powered recommendations (OpenAI + Pinecone RAG), and geospatial attraction search.

## Build & Run Commands

```bash
# Build (creates JAR in build/libs/)
./gradlew clean build

# Run locally (requires Redis + MySQL via Docker first)
docker compose up -d          # starts Redis:6379, MySQL:3307
./gradlew bootRun

# Run tests
./gradlew test

# Production deployment happens via GitHub Actions on push to `release` branch
```

**Note:** `application.yml` is gitignored — it's generated from GitHub Secrets during CI. For local dev, you must create `src/main/resources/application.yml` manually with the required properties (see `AppProperties.java` for shape: `app.auth.tokenSecret`, `app.auth.tokenExpirationMsec`, `app.oauth2.authorizedRedirectUris`, Redis/MySQL connection, AI keys).

## Architecture

### Package Structure: `com.practice.OAuth2`

- **`domain/`** — 6 bounded contexts, each with `controller/`, `service/`, `entity/`, `repository/`, `dto/` sub-packages:
  - `auth` — JWT issuance, refresh token rotation with Redisson distributed locking
  - `user` — user CRUD, OAuth2 provider integration
  - `plan` — trip plans, schedules, memos with real-time WebSocket (STOMP) updates
  - `attraction` — POI search with geohash clustering, MyBatis for spatial queries
  - `scrap` — URL metadata extraction (jsoup), bookmark folders
  - `ai` — RAG pipeline: query refinement → OpenAI embeddings → Pinecone vector search → GPT synthesis

- **`global/`** — cross-cutting concerns:
  - `config/` — SecurityConfig, RedisConfig, WebSocketConfig, S3Config
  - `security/` — JWT filter, OAuth2 handlers, `@CurrentUser` annotation
  - `exception/` — `GlobalExceptionHandler` + custom exception hierarchy
  - `common/` — `ApiResponse` wrapper, `BaseEntity` (JPA auditing)

### Key Patterns

- All entities extend `BaseEntity` (createdAt/updatedAt via `@EnableJpaAuditing`)
- Soft deletes via `@SQLDelete` + `@Where(clause = "deleted_at IS NULL")`
- Stateless JWT authentication; refresh tokens stored in Redis with RTR (Refresh Token Rotation)
- Distributed locking (Redisson) prevents token refresh stampede
- MyBatis used alongside JPA specifically for geospatial/clustering queries (`AttractionMapper.xml`)
- WebSocket endpoint at `/ws`, STOMP message broker on `/topic`
- Standard API response format: `ApiResponse<T>`

### Database

- MySQL 8.0 with ngram full-text parser for Korean text search
- Redis for refresh tokens and distributed locks
- Local dev: MySQL on port 3307 (`gmdb` / `admin2`), Redis on 6379

### Deployment

- Docker image: `eclipse-temurin:17-jdk-alpine`, timezone `Asia/Seoul`
- CI/CD: GitHub Actions → ECR push → S3 artifact → CodeDeploy to EC2
- Production runs two app replicas behind a shared Docker network

## Conventions

- Korean comments and commit messages are standard for this team
- Lombok everywhere: `@Getter`, `@Builder`, `@RequiredArgsConstructor`
- DTOs use `*Request` / `*Response` suffixes
- PR template expects: related issue, work description, Postman screenshots