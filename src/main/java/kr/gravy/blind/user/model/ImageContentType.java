package kr.gravy.blind.user.model;

import kr.gravy.blind.common.exception.BlindException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static kr.gravy.blind.common.exception.Status.UNSUPPORTED_FILE_TYPE;

/**
 * 허용된 이미지 Content-Type (화이트리스트 방식)
 */
@Slf4j
@Getter
public enum ImageContentType {
    JPEG("image/jpeg", ".jpg", ".jpeg"),  // RFC 1341: .jpg와 .jpeg 모두 허용
    PNG("image/png", ".png"),
    GIF("image/gif", ".gif"),
    WEBP("image/webp", ".webp");

    private final String mimeType;
    private final String primaryExtension;  // S3 키 생성용 기본 확장자
    private final String[] allExtensions;   // 검증용 모든 확장자

    ImageContentType(String mimeType, String... extensions) {
        this.mimeType = mimeType;
        this.primaryExtension = extensions[0];
        this.allExtensions = extensions;
    }

    /**
     * MIME type으로 ImageContentType 찾기
     *
     * @param mimeType MIME 타입 (예: "image/jpeg")
     * @return ImageContentType
     * @throws BlindException 지원하지 않는 파일 타입인 경우
     */
    public static ImageContentType fromMimeType(String mimeType) {
        log.debug("🔍 [ImageContentType] 검증 시작 - mimeType: '{}'", mimeType);
        for (ImageContentType type : values()) {
            if (type.mimeType.equals(mimeType)) {
                return type;
            }
        }
        log.error("[ImageContentType] 지원하지 않는 파일 형식 - mimeType: '{}'", mimeType);
        throw new BlindException(UNSUPPORTED_FILE_TYPE);
    }

    /**
     * 파일명이 올바른 확장자를 가졌는지 검증
     *
     * @param filename 원본 파일명
     * @return 확장자가 일치하면 true, 아니면 false
     */
    public boolean hasValidExtension(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();

        for (String ext : allExtensions) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * S3 키 생성용 기본 확장자 반환
     *
     * @return 기본 확장자 (예: ".jpg")
     */
    public String getExtension() {
        return primaryExtension;
    }
}
