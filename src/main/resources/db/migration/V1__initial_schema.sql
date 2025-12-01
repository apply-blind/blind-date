-- ========================================
-- V1__initial_schema.sql
-- FK 제약 조건 없음 (애플리케이션 제어)
-- DEFAULT 제약 조건 없음 (애플리케이션 제어)
-- ========================================

-- 1. users 테이블
CREATE TABLE users
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id        BINARY(16)   NOT NULL,
    provider         VARCHAR(50)  NOT NULL,
    provider_id      VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    grade            VARCHAR(20)  NOT NULL,
    has_base_profile BOOLEAN      NOT NULL,
    rejection_reason VARCHAR(500),
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,

    CONSTRAINT uq_users_provider_provider_id UNIQUE (provider, provider_id)
);

CREATE INDEX idx_users_public_id ON users (public_id);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_has_base_profile ON users (has_base_profile);

-- 2. admins 테이블
CREATE TABLE admins
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id  BINARY(16)   NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    grade      VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,

    CONSTRAINT uq_admins_username UNIQUE (username)
);

CREATE INDEX idx_admins_public_id ON admins (public_id);

-- 3. user_profiles 테이블 (승인된 데이터만, 닉네임 UNIQUE)
CREATE TABLE user_profiles
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    nickname           VARCHAR(10)  NOT NULL,
    gender             VARCHAR(10)  NOT NULL,
    birthday           DATE         NOT NULL,
    job_category       VARCHAR(20)  NOT NULL,
    job_title          VARCHAR(50)  NOT NULL,
    company            VARCHAR(100) NOT NULL,
    school             VARCHAR(100) NOT NULL,
    residence_city     VARCHAR(20)  NOT NULL,
    residence_district VARCHAR(50)  NOT NULL,
    work_city          VARCHAR(20)  NOT NULL,
    work_district      VARCHAR(50)  NOT NULL,
    height             INT          NOT NULL,
    blood_type         VARCHAR(5)   NOT NULL,
    body_type          VARCHAR(20)  NOT NULL,
    religion           VARCHAR(20)  NOT NULL,
    drinking           VARCHAR(20)  NOT NULL,
    smoking            VARCHAR(20)  NOT NULL,
    has_car            BOOLEAN      NOT NULL,
    introduction       TEXT         NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,

    CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uq_user_profiles_nickname UNIQUE (nickname)
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles (user_id);

-- 4. user_profile_personalities 테이블 (ElementCollection)
CREATE TABLE user_profile_personalities
(
    user_profile_id BIGINT      NOT NULL,
    personality     VARCHAR(50) NOT NULL
);

CREATE INDEX idx_user_profile_personalities_user_profile_id ON user_profile_personalities (user_profile_id);

-- 5. user_profiles_pending 테이블 (심사 대기)
CREATE TABLE user_profiles_pending
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    nickname           VARCHAR(10)  NOT NULL,
    gender             VARCHAR(10)  NOT NULL,
    birthday           DATE         NOT NULL,
    job_category       VARCHAR(20)  NOT NULL,
    job_title          VARCHAR(50)  NOT NULL,
    company            VARCHAR(100) NOT NULL,
    school             VARCHAR(100) NOT NULL,
    residence_city     VARCHAR(20)  NOT NULL,
    residence_district VARCHAR(50)  NOT NULL,
    work_city          VARCHAR(20)  NOT NULL,
    work_district      VARCHAR(50)  NOT NULL,
    height             INT          NOT NULL,
    blood_type         VARCHAR(5)   NOT NULL,
    body_type          VARCHAR(20)  NOT NULL,
    religion           VARCHAR(20)  NOT NULL,
    drinking           VARCHAR(20)  NOT NULL,
    smoking            VARCHAR(20)  NOT NULL,
    has_car            BOOLEAN      NOT NULL,
    introduction       TEXT         NOT NULL,
    requested_at       DATETIME     NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,

    CONSTRAINT uq_user_profiles_pending_user_id UNIQUE (user_id)
);

CREATE INDEX idx_user_profiles_pending_user_id ON user_profiles_pending (user_id);

-- 6. user_profile_personalities_pending 테이블 (ElementCollection)
CREATE TABLE user_profile_personalities_pending
(
    user_profile_pending_id BIGINT      NOT NULL,
    personality             VARCHAR(50) NOT NULL
);

CREATE INDEX idx_user_profile_personalities_pending_id ON user_profile_personalities_pending (user_profile_pending_id);

-- 7. user_images 테이블 (승인된 이미지만)
CREATE TABLE user_images
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id       BINARY(16)   NOT NULL,
    user_profile_id BIGINT       NOT NULL,
    s3_key          VARCHAR(500) NOT NULL,
    display_order   INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL
);

CREATE INDEX idx_user_images_public_id ON user_images (public_id);
CREATE INDEX idx_user_images_user_profile_id ON user_images (user_profile_id);
CREATE INDEX idx_user_images_display_order ON user_images (user_profile_id, display_order);

-- 8. user_images_pending 테이블
-- existing_image_id 컬럼: 프로필 수정 시 기존 이미지 참조용
CREATE TABLE user_images_pending
(
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id               BINARY(16)   NOT NULL,
    user_profile_pending_id BIGINT       NOT NULL,
    s3_key                  VARCHAR(500) NOT NULL,
    display_order           INT          NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    existing_image_id       BIGINT, -- 기존 이미지 참조 (EXISTING 타입일 때)
    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME     NOT NULL
);

CREATE INDEX idx_user_images_pending_public_id ON user_images_pending (public_id);
CREATE INDEX idx_user_images_pending_profile_id ON user_images_pending (user_profile_pending_id);
CREATE INDEX idx_user_images_pending_display_order ON user_images_pending (user_profile_pending_id, display_order);
CREATE INDEX idx_user_images_pending_existing_image_id ON user_images_pending (existing_image_id);

-- 9. refresh_tokens 테이블
CREATE TABLE refresh_tokens
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    user_type  VARCHAR(10)  NOT NULL,
    token      VARCHAR(500) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    expired_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

-- 10. notifications 테이블 (SINGLE_TABLE 상속 전략)
CREATE TABLE notifications
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    type           VARCHAR(255) NOT NULL,
    user_public_id BINARY(16)   NOT NULL,
    is_read        BOOLEAN      NOT NULL,
    reason         VARCHAR(500),
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL
);

CREATE INDEX idx_notifications_user_public_id ON notifications (user_public_id);
