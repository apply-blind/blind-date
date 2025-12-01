package kr.gravy.blind.infrastructure.aws;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.common.utils.GeneratorUtil;
import kr.gravy.blind.configuration.properties.S3Properties;
import kr.gravy.blind.user.model.ImageContentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;

import static kr.gravy.blind.common.exception.Status.*;

/**
 * AWS S3 서비스
 * 사용자 프로필 이미지 업로드/삭제 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * S3에서 파일을 삭제합니다.
     *
     * @param s3Key 삭제할 S3 객체 키
     */
    public void deleteFile(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            log.warn("S3 삭제 요청 키가 null 또는 빈 문자열입니다");
            throw new BlindException(INVALID_S3_URL);
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucketName())
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("S3 파일 삭제 성공 - key: {}", s3Key);
        } catch (S3Exception e) {
            log.error("S3 파일 삭제 실패: {}, 원인: {}", s3Key, e.getMessage());
            throw new BlindException(S3_DELETE_FAILED);
        }
    }

    /**
     * S3 파일에 대한 GET Presigned URL을 생성합니다. (다운로드용)
     * 시간 제한된 임시 URL로 보안을 강화합니다.
     *
     * @param s3Key S3 객체 키
     * @return 시간 제한된 Presigned URL (24시간)
     */
    public String generatePresignedUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            log.warn("Presigned URL 생성 요청 키가 null 또는 빈 문자열입니다");
            throw new BlindException(INVALID_S3_URL);
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.bucketName())
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(s3Properties.presignedUrlExpiration())
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            log.error("Presigned URL 생성 실패: {}, 원인: {}", s3Key, e.getMessage());
            throw new BlindException(S3_UPLOAD_FAILED);
        }
    }

    /**
     * S3 파일 업로드용 PUT Presigned URL을 생성합니다.
     * 클라이언트가 이 URL로 직접 S3에 업로드할 수 있습니다.
     *
     * @param s3Key       S3 객체 키
     * @param contentType 파일 MIME 타입 (예: "image/jpeg")
     * @return 업로드용 Presigned URL (10분 유효)
     */
    public String generatePutPresignedUrl(String s3Key, String contentType) {
        if (s3Key == null || s3Key.isBlank()) {
            log.warn("PUT Presigned URL 생성 요청 키가 null 또는 빈 문자열입니다");
            throw new BlindException(INVALID_S3_URL);
        }

        try {
            // 2025 베스트 프랙티스: ACL 제거, Bucket Policy 사용
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucketName())
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))  // 10분 유효
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            log.error("PUT Presigned URL 생성 실패: {}, 원인: {}", s3Key, e.getMessage());
            throw new BlindException(S3_UPLOAD_FAILED);
        }
    }


    /**
     * S3 객체 키를 생성합니다.
     *
     * @param originalFilename 원본 파일명
     * @param contentType      MIME 타입 (예: "image/jpeg")
     * @return S3 객체 키 (예: "user-profiles/550e8400-e29b-41d4-a716-446655440000.jpg")
     */
    public String generateS3Key(String originalFilename, String contentType) {
        log.debug("🔍 [S3Service] S3 Key 생성 시작 - filename: '{}', contentType: '{}'",
            originalFilename, contentType);

        // 1. Content-Type 화이트리스트 검증
        ImageContentType imageType = ImageContentType.fromMimeType(contentType);

        // 2. 확장자와 Content-Type 일치 검증 (보안)
        if (!imageType.hasValidExtension(originalFilename)) {
            log.error("[S3Service] 확장자 불일치 - filename: '{}', expected: '{}', contentType: '{}'",
                originalFilename, imageType.getExtension(), contentType);
            throw new BlindException(FILE_EXTENSION_MISMATCH);
        }

        // 3. S3 Key 생성 (AWS 베스트 프랙티스: 폴더 + UUID v7 + 확장자)
        // Java 17 JEP 280: + 연산자가 자동으로 invokedynamic으로 최적화됨
        String s3Key = s3Properties.folderName() + "/" +
                GeneratorUtil.generatePublicId() +
                imageType.getExtension();

        log.debug("✅ [S3Service] S3 Key 생성 완료 - s3Key: '{}'", s3Key);
        return s3Key;
    }

    /**
     * S3 객체가 존재하는지 확인합니다.
     *
     * @param s3Key S3 객체 키
     * @return 존재하면 true, 존재하지 않으면 false
     */
    public boolean exists(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return false;
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.bucketName())
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            log.debug("S3 객체가 존재하지 않음: {}", s3Key);
            return false;
        } catch (S3Exception e) {
            log.error("S3 객체 존재 확인 실패: {}, 원인: {}", s3Key, e.getMessage());
            throw new BlindException(S3_UPLOAD_FAILED);
        }
    }


    /**
     * 여러 S3 파일을 삭제합니다
     * 일부 실패해도 계속 진행하며, 실패한 파일은 로깅만 합니다.
     *
     * @param s3Keys 삭제할 S3 객체 키 리스트
     */
    public void deleteMultipleFiles(List<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) {
            return;
        }

        for (String s3Key : s3Keys) {
            try {
                if (s3Key != null && !s3Key.isBlank()) {
                    deleteFile(s3Key);
                    log.debug("S3 파일 삭제 성공 - s3Key: {}", s3Key);
                }
            } catch (Exception e) {
                // 실패는 로깅만 하고 계속 진행 (고아 파일 발생 가능, 추후 cleanup job으로 처리)
                log.error("S3 파일 삭제 실패 (고아 파일 발생) - s3Key: {}, 원인: {}",
                        s3Key, e.getMessage());
            }
        }

    }
}
