-- ===========================================================================================
-- FK 제약 제거, 인덱스 유지, UNIQUE 최소화)
-- ===========================================================================================

CREATE TABLE anonymous_posts
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id          BINARY(16)   NOT NULL,
    user_id            BIGINT       NOT NULL,
    author_gender      VARCHAR(10)  NOT NULL COMMENT '작성자 성별 (MALE, FEMALE)',
    anonymous_nickname VARCHAR(50)  NOT NULL COMMENT '익명 닉네임 (게시글 작성자)',
    category           VARCHAR(20)  NOT NULL,
    title              VARCHAR(255) NOT NULL,
    content            TEXT         NOT NULL,
    view_count         INT          NOT NULL,
    like_count         INT          NOT NULL,
    comment_count      INT          NOT NULL,
    is_pinned          BOOLEAN      NOT NULL,
    is_hot             BOOLEAN      NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,

    INDEX              idx_anonymous_posts_public_id (public_id),
    INDEX              idx_anonymous_posts_user_id_created (user_id, created_at DESC),
    INDEX              idx_anonymous_posts_category_created_at (category, created_at DESC),
    INDEX              idx_anonymous_posts_category_pinned (category, is_pinned DESC, created_at DESC),
    INDEX              idx_anonymous_posts_hot (is_hot, created_at DESC)
);


CREATE TABLE anonymous_post_images
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id      BIGINT       NOT NULL,
    s3_key       VARCHAR(500) NOT NULL COMMENT 'S3 객체 키',
    content_type VARCHAR(50)  NOT NULL COMMENT 'MIME 타입',
    status       VARCHAR(20)  NOT NULL COMMENT '업로드 상태: NOT_UPLOADED, UPLOADED',
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,

    INDEX        idx_anonymous_post_images_post_id (post_id)
);

CREATE TABLE anonymous_post_likes
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    post_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT uq_anonymous_post_likes_user_post UNIQUE (user_id, post_id),

    INDEX      idx_anonymous_post_likes_user_id (user_id),
    INDEX      idx_anonymous_post_likes_post_id (post_id)
);


CREATE TABLE anonymous_comments
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '댓글 ID (PK)',
    public_id          BINARY(16)   NOT NULL COMMENT '공개 ID (UUID v7, 외부 노출용)',
    post_id            BIGINT      NOT NULL COMMENT '게시글 ID',
    user_id            BIGINT      NOT NULL COMMENT '작성자 ID',
    author_gender      VARCHAR(10) NOT NULL COMMENT '작성자 성별 (MALE, FEMALE)',
    anonymous_nickname VARCHAR(50) NOT NULL COMMENT 'HMAC 기반 익명 닉네임 (게시글별 고정)',
    parent_comment_id  BIGINT NULL     COMMENT 'Self-Referencing: NULL이면 최상위 댓글, NOT NULL이면 대댓글',
    content            TEXT        NOT NULL COMMENT '댓글 내용',
    like_count         INT         NOT NULL COMMENT '좋아요 수 (비정규화, 읽기 성능 최적화)',
    status             VARCHAR(20) NOT NULL COMMENT '댓글 상태 (ACTIVE: 활성, DELETED: 삭제)',
    created_at         DATETIME    NOT NULL COMMENT '생성 시각',
    updated_at         DATETIME    NOT NULL COMMENT '수정 시각'
);

CREATE INDEX idx_comments_public_id ON anonymous_comments (public_id);
CREATE INDEX idx_comments_post_id_parent_null ON anonymous_comments (post_id, parent_comment_id, created_at ASC);
CREATE INDEX idx_comments_parent_id ON anonymous_comments (parent_comment_id, created_at ASC);
CREATE INDEX idx_comments_user_id ON anonymous_comments (user_id, created_at DESC);
CREATE INDEX idx_comments_post_nickname_status ON anonymous_comments (post_id, anonymous_nickname, status);


CREATE TABLE anonymous_comment_likes
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '좋아요 ID (PK)',
    user_id    BIGINT   NOT NULL COMMENT '사용자 ID',
    comment_id BIGINT   NOT NULL COMMENT '댓글 ID',
    created_at DATETIME NOT NULL COMMENT '생성 시각',
    updated_at DATETIME NOT NULL COMMENT '수정 시각',

    CONSTRAINT uq_comment_likes_user_comment UNIQUE (user_id, comment_id)
);

CREATE INDEX idx_comment_likes_comment_id ON anonymous_comment_likes (comment_id);

ALTER TABLE notifications
    ADD COLUMN post_public_id BINARY(16) NULL COMMENT '게시글 Public ID',
    ADD COLUMN post_title VARCHAR(200) NULL COMMENT '게시글 제목',
    ADD COLUMN comment_content TEXT NULL COMMENT '댓글 내용 미리보기',
    ADD COLUMN comment_public_id BINARY(16) NULL COMMENT '댓글/대댓글 Public ID (중복 방지용)';

CREATE INDEX idx_notifications_user_created ON notifications (user_public_id, created_at DESC);
CREATE UNIQUE INDEX idx_notifications_user_comment_type ON notifications (user_public_id, comment_public_id, type);
