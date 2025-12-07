package kr.gravy.blind.common.type;

import lombok.Getter;

@Getter
public enum ImageSize {

    THUMBNAIL(200, "목록 조회용 썸네일"),
    MEDIUM(800, "상세 조회용 중간 크기"),
    FULL(1920, "원본 크기");

    private final int width;
    private final String description;

    ImageSize(int width, String description) {
        this.width = width;
        this.description = description;
    }

    /**
     * @param cloudFrontDomain CloudFront 도메인 (예: d111111abcdef8.cloudfront.net)
     * @param s3Key            S3 객체 키 (예: images/profile/uuid.jpg)
     * @return CDN URL with query params (예: https://d111111abcdef8.cloudfront.net/images/profile/uuid.jpg?width=200&format=auto)
     */
    public String buildCdnUrl(String cloudFrontDomain, String s3Key) {
        // format=auto: CloudFront가 브라우저에 맞는 최적 포맷 자동 선택 (WebP, AVIF 등)
        return String.format("https://%s/%s?width=%d&format=auto",
                cloudFrontDomain, s3Key, this.width);
    }
}
