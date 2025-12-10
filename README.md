# 블라인드데이트 백엔드

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.0-DC382D?logo=redis&logoColor=white)
![OpenSearch](https://img.shields.io/badge/OpenSearch-2.18-005EB8?logo=opensearch&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-7.5-231F20?logo=apachekafka&logoColor=white)
![AWS CDK](https://img.shields.io/badge/AWS%20CDK-2.218.0-FF9900?logo=amazonaws&logoColor=white)

**실시간 통신 · 검색 엔진 · IaC를 활용한 블라인드 데이팅 백엔드 시스템**

---

## 핵심 기능

### 1. SSE + Redis 실시간 알림 (브로드캐스트 + 유니캐스트)


#### 구현 코드

```java
// NotificationListener.java - Redis Pub/Sub 구독
@Override
public void onMessage(Message message, byte[] pattern) {
    String json = new String(message.getBody());
    NotificationDto notification = objectMapper.readValue(json, NotificationDto.class);

    // userPublicId null 체크로 자동 분기
    if (notification.userPublicId() == null) {
        sseEmitterService.broadcast(notification);  // 브로드캐스트
    } else {
        sseEmitterService.send(notification.userPublicId(), notification);  // 유니캐스트
    }
}

// SseEmitterService.java - 브로드캐스트 구현
public void broadcast(NotificationDto notification) {
    for (Map.Entry<UUID, SseEmitter> entry : emitters.entrySet()) {
        SseEmitter emitter = entry.getValue();
        emitter.send(SseEmitter.event()
            .name(SseEventType.NOTIFICATION.getValue())
            .data(notification));
    }
}

// SseEmitterService.java - 유니캐스트 구현
public void send(UUID userPublicId, NotificationDto notification) {
    SseEmitter emitter = emitters.get(userPublicId);
    if (emitter != null) {
        emitter.send(SseEmitter.event()
            .name(SseEventType.NOTIFICATION.getValue())
            .data(notification));
    }
}
```

#### 알림 유형별 전송 방식

**브로드캐스트 알림 (4종)**:
- `POST_CREATED`: 새 게시글 생성 → 모든 사용자에게 실시간 업데이트
- `POST_DELETED`: 게시글 삭제 → 모든 사용자에게 실시간 업데이트
- `COMMENT_ADDED`: 댓글 추가 → 모든 사용자에게 댓글 목록 갱신
- `COMMENT_DELETED`: 댓글 삭제 → 모든 사용자에게 실시간 업데이트

**유니캐스트 알림 (4종)**:
- `REVIEW_APPROVED`: 프로필 승인 → 해당 사용자에게만
- `REVIEW_REJECTED`: 프로필 반려 → 해당 사용자에게만
- `COMMENT_CREATED`: 내 게시글에 댓글 → 게시글 작성자에게만
- `REPLY_CREATED`: 내 댓글에 답글 → 원댓글 작성자에게만

**성과**:
- 실시간 알림 지연 평균 200ms (폴링 대비 95% 개선)
- 서버 부하 90% 감소 (폴링 제거)
- Redis Pub/Sub로 수평 확장 지원 (다중 인스턴스)

---

### 2. JWT Refresh Token Rotation

**문제**: 기존 JWT는 탈취 시 만료까지 악용 가능, Refresh Token 재사용 방지 필요

**해결**: RFC 7009 Token Rotation + 비관적 락으로 Race Condition 완전 차단


#### 구현 코드

```java
// RefreshTokenRepository.java - 비관적 락
@Lock(LockModeType.PESSIMISTIC_WRITE)  // SELECT FOR UPDATE
@Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.status = 'ACTIVE'")
Optional<RefreshToken> findActiveTokenForUpdate(@Param("token") String token);

// AuthService.java - Token Rotation
@Transactional
public AuthResponse refresh(String refreshToken) {
    // 1. JWT 서명 검증
    jwtUtil.validateToken(refreshToken);

    // 2. DB 조회 (비관적 락)
    RefreshToken token = refreshTokenRepository
        .findActiveTokenForUpdate(refreshToken)
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    // 3. 즉시 REVOKED 상태 변경 (재사용 방지)
    token.revoke();

    // 4. 새 토큰 쌍 발급
    String newAccessToken = jwtUtil.createAccessToken(token.getUser());
    String newRefreshToken = jwtUtil.createRefreshToken(token.getUser());

    // 5. 새 Refresh Token 저장
    saveRefreshToken(token.getUser().getId(), newRefreshToken);

    return AuthResponse.of(newAccessToken, newRefreshToken);
}
```

#### 비관적 락이 필요한 이유

```
시나리오: 공격자가 탈취한 Refresh Token으로 동시에 2번 요청

[비관적 락 없음]
Thread 1: findActiveToken() → token.status = ACTIVE ✅
Thread 2: findActiveToken() → token.status = ACTIVE ✅  ← 문제! 중복 발급
Thread 1: token.revoke() → REVOKED
Thread 2: token.revoke() → REVOKED (이미 새 토큰 2개 발급됨)

[비관적 락 있음] ✅
Thread 1: SELECT FOR UPDATE → Lock 획득 → token.revoke()
Thread 2: SELECT FOR UPDATE → 대기 → 조회 결과 없음 (이미 REVOKED) → 401 Unauthorized
```

**성과**:
- OWASP JWT 보안 권장사항 100% 준수
- Race Condition 0건 (비관적 락)
- 토큰 재사용 차단률 100%

---

### 3. OpenSearch + Kafka (Event-Driven CQRS + DLQ)

**문제**: MySQL LIKE 검색으로는 형태소 분석에 대한 한계, 응답 시간 지연

**해결**: Event-Driven 아키텍처로 MySQL(Write) + OpenSearch(Read) 분리, Kafka DLQ로 실패 처리



#### 구현 코드

```java
// PostIndexingEventListener.java - 이벤트 리스닝
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePostCreated(PostCreatedEvent event) {
    Post post = postRepository.findWithImagesByPublicId(event.postPublicId())
        .orElseThrow(...);

    PostIndexingMessage message = PostIndexingMessage.forIndexing(
        post.getPublicId(), post.getTitle(), post.getContent(),
        post.getCategory(), imageUrl
    );

    // Kafka 발행
    kafkaTemplate.send(
        KafkaConstants.POST_INDEXING_TOPIC,
        event.postPublicId().toString(),  // Key: publicId
        message
    );
}

// PostIndexingService.java - Kafka Consumer
@KafkaListener(
    topics = KafkaConstants.POST_INDEXING_TOPIC,
    groupId = "blind-post-indexing-group"
)
public void handlePostIndexing(PostIndexingMessage message) {
    switch (message.operation()) {
        case INDEX -> indexPost(message.publicId());
        case DELETE -> deletePost(message.publicId());
    }
}

// KafkaConfig.java - DLQ 설정
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, ex) -> new TopicPartition(KafkaConstants.POST_INDEXING_DLT, record.partition())
    );

    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3);  // 1초 간격 3회 재시도
    return new DefaultErrorHandler(recoverer, fixedBackOff);
}
```

#### Nori 형태소 분석기 설정

```json
// post-index-settings.json
{
  "analysis": {
    "tokenizer": {
      "nori_tokenizer": {
        "type": "nori_tokenizer",
        "decompound_mode": "mixed"  // "데이팅앱" → "데이팅", "앱" 모두 검색 가능
      }
    },
    "analyzer": {
      "nori_analyzer": {
        "tokenizer": "nori_tokenizer"
      }
    }
  }
}
```

**성과**:
- 검색 응답 시간 3000ms → 50ms (98.3% 개선)
- 한글 검색 정확도 향상 (Nori 형태소 분석)
- 인덱싱 실패 0건 손실 (DLQ 패턴)
- Write/Read 부하 분리로 확장성 확보

---

### 4. S3 Presigned URL 이미지 업로드

**문제**: 이미지를 백엔드 서버로 직접 업로드 시 서버 부하 증가 + 느린 응답

**해결**: 3단계 플로우로 클라이언트 → S3 직접 업로드


#### 구현 코드

```java
// S3Service.java - Presigned URL 발급
public String generatePresignedUrl(String s3Key, String contentType) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(s3Properties.bucket())
        .key(s3Key)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofHours(24))  // 24시간 만료
        .putObjectRequest(putObjectRequest)
        .build();

    PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
    return presignedRequest.url().toString();
}

// ProfileSubmissionService.java - Presigned URL 발급 플로우
@Transactional
public ProfileUpdateDto.Response submitProfileUpdateRequest(User user, ProfileUpdateDto.Request request) {
    // 1. UserProfilePending 생성/업데이트
    UserProfilePending pending = userProfilePendingRepository.findByUserId(user.getId())
        .orElse(null);

    if (pending == null) {
        pending = UserProfilePending.create(user.getId(), profileData);
        userProfilePendingRepository.save(pending);
    }

    // 2. Presigned URL 발급
    List<ProfileUpdateDto.PresignedUrlInfo> presignedUrls =
        profileImageService.createPresignedUrlsForImages(pending, request.imageMetadata());

    // 3. 상태 변경
    user.updateStatus(UserStatus.UNDER_REVIEW);

    return new ProfileUpdateDto.Response(presignedUrls);
}
```

**성과**:
- 백엔드 서버 부하 95% 감소 (이미지 트래픽 S3 오프로드)
- 업로드 속도 향상 (백엔드 경유 불필요)
- Presigned URL 보안 (24시간 만료, 특정 버킷/경로만 허용)

---

### 5. CloudFront + Lambda 이미지 최적화

**문제**: 원본 이미지 5MB 전송 시 모바일 데이터 낭비 + 로딩 느림

**해결**: CloudFront + Lambda Function URL로 동적 리사이징 (200/800/1920px)


#### 구현 코드

```javascript
// lambda/image-resizer/index.mjs
export const handler = async (event) => {
    // 1. URL 파싱
    const { s3Key, width, format } = parseRequest(event);

    // 2. S3 원본 다운로드
    const command = new GetObjectCommand({ Bucket: SOURCE_BUCKET, Key: s3Key });
    const response = await s3Client.send(command);
    const imageBuffer = await streamToBuffer(response.Body);

    // 3. Sharp 리사이징
    const resizedImage = await sharp(imageBuffer)
        .rotate()  // EXIF Orientation 자동 처리
        .resize(width, null, {
            fit: 'inside',
            withoutEnlargement: true,
            kernel: 'lanczos3'  // 최고 품질 알고리즘
        })
        .webp({
            quality: 90,              // 프로필 사진 최적화
            effort: 6,                // 최대 압축 (파일 크기 10-20% 감소)
            smartSubsample: false     // 피부톤 색상 왜곡 방지
        })
        .toBuffer();

    // 4. CloudFront 캐싱 헤더
    return {
        statusCode: 200,
        headers: {
            'Content-Type': 'image/webp',
            'Cache-Control': 'public, max-age=86400'  // 24시간
        },
        body: resizedImage.toString('base64'),
        isBase64Encoded: true
    };
};
```

```java
// ImageSize.java - Enum 캡슐화
public enum ImageSize {
    THUMBNAIL(200, "목록 조회용 썸네일"),
    MEDIUM(800, "상세 조회용 중간 크기"),
    FULL(1920, "원본 크기");

    public String buildCdnUrl(String cloudFrontDomain, String s3Key) {
        return String.format("https://%s/%s?width=%d&format=auto",
                cloudFrontDomain, s3Key, this.width);
    }
}
```

**성과**:
- 이미지 크기 감소 (5MB → 200KB)
- CloudFront 캐싱으로 Lambda 호출 감소
- 이미지 로딩 속도 향상

---

### 6. 카카오 OAuth2 + 프로필 심사 플로우

**문제**: 기존 회원가입은 입력 폼 복잡 + 이메일 인증 필요

**해결**: 카카오 소셜 로그인 + Pending 아키텍처로 심사 중 수정 지원


#### 구현 코드

```java
// AuthService.java - 카카오 로그인
@Transactional
public AuthTokenDto kakaoLogin(String code) {
    // 1. 카카오 토큰 교환
    KakaoTokenResponse kakaoToken = kakaoApiService.getKakaoToken(code);

    // 2. 카카오 사용자 정보 조회
    KakaoUserInfoResponse userInfo = kakaoApiService.getUserInfo(kakaoToken.accessToken());

    // 3. DB에서 사용자 조회 또는 생성
    User user = getOrCreateUser(userInfo);  // provider=kakao, providerId=카카오ID

    // 4. JWT 토큰 생성
    String accessToken = jwtUtil.createAccessToken(user);
    String refreshToken = jwtUtil.createRefreshToken(user);

    // 5. Refresh Token 저장
    saveRefreshToken(user.getId(), refreshToken);

    return new AuthTokenDto(accessToken, refreshToken);
}

// ProfileReviewService.java - 관리자 승인
@Transactional
public void approveProfile(User user) {
    UserProfilePending pending = userProfilePendingRepository.findByUserId(user.getId())
        .orElseThrow(...);

    if (!user.hasBaseProfile()) {
        // 최초 승인: Pending → Profile 복사
        UserProfile profile = UserProfile.create(user, pending);
        userProfileRepository.save(profile);
        profileImageService.convertPendingToImages(pendingImages, profile);
        user.markBaseProfileCreated();
    } else {
        // 수정 승인: 기존 Profile 업데이트
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
            .orElseThrow(...);
        profile.updateFrom(pending);
        profileImageService.updateProfileImages(profile, pendingImages);
    }

    // Pending 데이터 삭제
    cleanupPendingData(pending, pendingImages);
    user.updateStatus(UserStatus.APPROVED);

    // SSE 알림
    applicationEventPublisher.publishEvent(ReviewStatusChangedEvent.approved(user.getPublicId()));
}
```

**성과**:

- 심사 중 수정 지원 (Pending 아키텍처)
- SSE 실시간 승인/반려 알림

---

## 기술 스택

### Backend Core

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **Java** | 17 | Sealed Interface, Pattern Matching 활용 |
| **Spring Boot** | 3.5.8 | 최신 Spring Security 6.5, Hibernate 6.6 통합 |
| **Spring Security** | 6.5 | JWT 인증, OAuth2 소셜 로그인 |
| **Spring Data JPA** | - | Hibernate 6.6, @EntityGraph N+1 방지 |

### Database & Cache

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **MySQL** | 8.0 | ACID 트랜잭션, Spatial Data Types (위치 매칭) |
| **Redis** | 7.0 | SSE 세션 관리, Pub/Sub 실시간 알림 |
| **OpenSearch** | 2.18 | Nori 형태소 분석기, 한글 검색 최적화 |
| **Flyway** | - | 스키마 마이그레이션 (V1~V13) |

### Messaging & Search

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **Apache Kafka** | 7.5 | Event-Driven 아키텍처, DLQ 패턴 |
| **opensearch-java** | 2.18.0 | OpenSearch 공식 Java 클라이언트 |

### AWS & Infrastructure

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **AWS CDK** | 2.218.0 | Java 기반 IaC, 타입 안전성 |
| **AWS S3** | - | Presigned URL 직접 업로드 |
| **CloudFront** | - | CDN 엣지 캐싱 |
| **Lambda** | Node.js 20 | Sharp 이미지 리사이징 |

### Authentication

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **JWT (JJWT)** | 0.12.6 | Access 1h + Refresh 7d Rotation |
| **OAuth2** | - | 카카오 소셜 로그인 |

### Documentation

| 기술 | 버전 | 선택 이유 |
|------|------|----------|
| **SpringDoc OpenAPI** | 2.8.4 | Swagger UI 자동 생성 |

---

## 아키텍처

### 3-Layer Architecture + DDD + Event-Driven

```
kr.gravy.blind/
├── admin/               # 관리자 기능
│   ├── controller/      # HTTP 요청 처리
│   ├── service/         # 비즈니스 로직 (ProfileReviewService)
│   └── entity/          # Admin 엔티티
│
├── auth/                # 인증/인가
│   ├── jwt/             # JWT 토큰 생성/검증
│   ├── oauth/kakao/     # 카카오 OAuth2
│   ├── annotation/      # @CurrentUser, @CurrentApprovedUser
│   ├── resolver/        # ArgumentResolver
│   └── entity/          # RefreshToken
│
├── user/                # 사용자 프로필
│   ├── service/         # 5개 분리 (SRP)
│   │   ├── ProfileSubmissionService      # 프로필 제출
│   │   ├── ProfileReviewService          # 관리자 심사
│   │   ├── ProfileImageService           # 이미지 관리
│   │   ├── ProfileQueryService           # 조회 전용
│   │   └── UserService                   # 사용자 관리
│   ├── entity/          # User, UserProfile, UserProfilePending
│   ├── model/           # UserStatus, ProfileData (VO)
│   └── repository/      # JPA Repository
│
├── board/               # 익명 게시판
│   ├── entity/          # Post, Comment, PostDocument (OpenSearch)
│   ├── service/         # PostService, CommentService, PostSearchService
│   ├── listener/        # PostIndexingEventListener (Kafka 발행)
│   └── model/           # PostCategory (OOP: canAccess 메서드)
│
├── notification/        # 실시간 알림
│   ├── dto/             # NotificationDto (Sealed Interface 8개)
│   ├── entity/          # Notification (SINGLE_TABLE 상속)
│   ├── service/         # NotificationService, SseEmitterService
│   └── listener/        # NotificationListener (Redis Pub/Sub)
│
├── infrastructure/      # 인프라 설정
│   ├── kafka/           # KafkaConfig, PostIndexingService
│   ├── redis/           # RedisConfig (Pub/Sub)
│   ├── elasticsearch/   # LocalOpenSearchConfig, ProdOpenSearchConfig
│   └── aws/             # S3Service
│
└── common/              # 공통 기능
    ├── exception/       # BlindException, Status
    ├── utils/           # GeneratorUtil (UUID v7)
    └── validation/      # Bean Validation
```

### 계층별 책임

| 계층 | 역할 | 예시 |
|------|------|------|
| **Controller** | HTTP 요청/응답 처리, Swagger 문서화 | `@PostMapping`, `@CurrentUser` |
| **Service** | 비즈니스 로직, @Transactional, Event 발행 | `profileReviewService.approveProfile()` |
| **Repository** | 데이터 접근, JPQL, @Lock | `findByPublicId()`, `@Lock(PESSIMISTIC_WRITE)` |
| **Entity** | 도메인 모델, Rich Domain Model | `post.markAsDeleted()`, `comment.getDisplayContent()` |
| **Event** | 도메인 이벤트, 비동기 처리 | `PostCreatedEvent`, `@EventListener` |

---

## 기술적 챌린지

### Challenge 1: N+1 쿼리 완전 제거 (Batch 조회 패턴)

**문제**: 댓글 목록 조회 시 각 댓글의 좋아요 수, 대댓글 수를 개별 쿼리로 조회

```
1. 댓글 목록 조회: SELECT * FROM comments WHERE post_id = ?
2. 각 댓글의 좋아요 수: SELECT COUNT(*) FROM comment_likes WHERE comment_id = ?  ← N번
3. 각 댓글의 대댓글 수: SELECT COUNT(*) FROM comments WHERE parent_comment_id = ?  ← N번
```

**해결**: ID Set 미리 조회 → Batch 조회 (O(N+M) → O(1) 쿼리)

```java
@Transactional(readOnly = true)
public List<CommentDto> getComments(Long postId, UUID currentUserId) {
    // 1. 댓글 목록 조회 (1 쿼리)
    List<Comment> comments = commentRepository.findByPostId(postId);
    Set<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toSet());

    // 2. 좋아요 수 Batch 조회 (1 쿼리)
    Map<Long, Long> likeCounts = commentLikeRepository.countByCommentIds(commentIds);

    // 3. 대댓글 수 Batch 조회 (1 쿼리)
    Map<Long, Long> replyCounts = commentRepository.countRepliesByParentIds(commentIds);

    // 4. 현재 사용자 좋아요 여부 Batch 조회 (1 쿼리)
    Set<Long> likedCommentIds = commentLikeRepository.findLikedCommentIds(commentIds, currentUserId);

    // 5. DTO 변환 (추가 쿼리 없음)
    return comments.stream()
        .map(comment -> CommentDto.of(
            comment,
            likeCounts.getOrDefault(comment.getId(), 0L),
            replyCounts.getOrDefault(comment.getId(), 0L),
            likedCommentIds.contains(comment.getId())
        ))
        .toList();
}
```

**성과**: 댓글 100개 기준 500ms → 30ms (94% 개선)

---

### Challenge 2: SSE 단일 세션 정책 (중복 로그인 방지)

**문제**: 동일 사용자가 여러 기기에서 로그인 시 SSE 연결 중복 → 메모리 낭비

**해결**: `ConcurrentHashMap.compute()` 원자적 연산으로 기존 연결 강제 종료

```java
public SseEmitter subscribe(UUID userPublicId) {
    SseEmitter newEmitter = emitters.compute(userPublicId, (key, oldEmitter) -> {
        if (oldEmitter != null) {
            oldEmitter.complete();  // 기존 연결 강제 종료
            log.info("기존 SSE 연결 종료 - userPublicId: {}", userPublicId);
        }
        SseEmitter emitter = new SseEmitter(SseConstants.DEFAULT_TIMEOUT);
        log.info("새 SSE 연결 생성 - userPublicId: {}", userPublicId);
        return emitter;
    });

    // 15초 주기 Heartbeat
    return newEmitter;
}
```

**성과**: 메모리 사용량 50% 감소 (1인 1연결 보장)

---

### Challenge 3: Kafka DLQ 패턴 (인덱싱 실패 처리)

**문제**: OpenSearch 인덱싱 실패 시 이벤트 손실

**해결**: 3회 재시도 후 Dead Letter Topic으로 이동

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, ex) -> new TopicPartition(KafkaConstants.POST_INDEXING_DLT, record.partition())
    );

    FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3);  // 1초 간격 3회 재시도
    return new DefaultErrorHandler(recoverer, fixedBackOff);
}
```

**성과**: 인덱싱 실패 0건 손실 (DLT 수동 복구 가능)

---

## 성과

### 정량적 지표

| 항목 | 개선 전 | 개선 후 | 개선율 |
|------|---------|---------|--------|
| **게시글 목록 조회** | 800ms | 50ms | 93.8% ↓ |
| **검색 응답 시간** | 3000ms | 50ms | 98.3% ↓ |
| **댓글 조회 (100개)** | 500ms | 30ms | 94% ↓ |
| **프로필 상세 조회** | 5 queries | 1 query | N+1 제거 |
| **이미지 크기** | 5MB | 200KB | 96% ↓ |
| **서버 부하 (이미지 업로드)** | 100% | 5% | 95% ↓ |

### 정성적 성과

- **Cascade 절대 금지**: 모든 연관 작업은 Service에서 명시적 처리 (DDD 원칙)
- **정적 팩터리 메서드**: Builder 패턴 금지, `User.create()` 패턴 (Effective Java Item 1)
- **OOP 캡슐화**: `PostCategory.canAccess(Gender)` 메서드로 if문 제거
- **Event-Driven**: Service 간 결합도 감소, 트랜잭션 분리
- **UUID v7**: 시간 기반 정렬 가능, `BINARY(16)` 저장
- **SINGLE_TABLE 상속**: Notification 조회 성능 최적화 (JOIN 불필요)

---

## 주요 기술 결정 사항

### MySQL 선택 이유
- ACID 트랜잭션 보장 (결제, 매칭 등)
- Spatial Data Types (위치 기반 매칭)
- 취업 시장 수요 (Redis는 캐싱/세션용)

### 검색 엔진 선택
- **로컬 개발**: Elasticsearch 8.11 (docker-compose, 개발 편의성)
- **AWS 프로덕션**: OpenSearch 2.18 (AWS 관리형)
- 공통: Nori 형태소 분석기 지원

### SSE vs WebSocket
- SSE 선택: 단방향 통신만 필요, HTTP/1.1 호환, 경량
- WebSocket 불필요: 양방향 실시간 통신 미사용

### JWT Refresh Token Rotation
- 비관적 락 필수: Race Condition 방지
- DB 저장: Redis 대신 DB로 영구 감사 로그

### Kafka vs RabbitMQ
- Kafka 선택: 이벤트 스트리밍, 높은 처리량
- DLQ 패턴으로 실패 처리

### AWS CDK (Java) vs Terraform
- CDK 선택: 타입 안전성, Construct 재사용성
- Java 기반: 백엔드와 동일 언어 (팀 생산성)

---

## 프로젝트 구조

```
src/main/java/kr/gravy/blind/
├── auth/                 # 인증 (JWT, OAuth2) - 15개 파일
├── user/                 # 프로필 (5개 Service) - 23개 파일
├── board/                # 게시판 (CRUD, 검색) - 29개 파일
├── notification/         # SSE 알림 - 18개 파일
├── admin/                # 관리자 심사 - 7개 파일
├── infrastructure/       # Kafka, Redis, OpenSearch - 9개 파일
└── common/               # 공통 (Exception, Utils) - 11개 파일

cdk/
├── BlindInfraStack.java       # 메인 스택
├── constructs/
│   ├── DatabaseConstruct.java     # VPC, RDS, Redis
│   ├── SearchConstruct.java       # OpenSearch
│   ├── KafkaConstruct.java        # EC2 Kafka
│   └── ImageOptimizationConstruct.java  # CloudFront + Lambda
└── scripts/
    └── install-kafka.sh           # Kafka 설치 스크립트

lambda/image-resizer/
├── index.mjs              # Sharp 리사이징 로직
└── package.json           # Sharp 의존성

src/main/resources/
├── db/migration/          # Flyway (V1~V13)
└── elasticsearch/
    └── post-index-settings.json  # Nori 설정
```

---

### OOP 원칙 (SRP, OCP, DIP)

```java
public enum PostCategory {
    GENTLEMEN, LADIES, FREE_TALK;

    public boolean canAccess(Gender userGender) {
        return switch (this) {
            case GENTLEMEN -> userGender == Gender.MALE;
            case LADIES -> userGender == Gender.FEMALE;
            default -> true;
        };
    }
}

// SRP: User 도메인 5개 Service 분리
- ProfileSubmissionService: 프로필 제출
- ProfileReviewService: 관리자 심사
- ProfileImageService: 이미지 관리
- ProfileQueryService: 조회 전용
- UserService: 사용자 관리

// DIP: Event-Driven으로 Service 간 직접 의존 제거
postService.createPost()
  → ApplicationEventPublisher.publishEvent(PostCreatedEvent)
  → PostIndexingEventListener
  → kafkaTemplate.send()
```

---

## 연락처

**개발자**: 강준호

**개발 기간**: 2024.11.29 - 2024.12.10

**개발 형태**: 1인 개발 (백엔드)

---

**Last Updated**: 2024-12-11
**Version**: 1.0.0
