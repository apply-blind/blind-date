package kr.gravy.blind.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum Status {

    BAD_REQUEST(400, "bad_request", "잘못된 요청입니다"),
    INTERNAL_SERVER_ERROR(500, "server001", "서버 내부 오류가 발생했습니다"),

    // === 인증 관련 ===
    AUTHENTICATION_FAILED(401, "auth001", "인증에 실패했습니다"),
    TOKEN_EXPIRED(401, "auth003", "토큰이 만료되었습니다"),
    INVALID_TOKEN(401, "auth004", "유효하지 않거나 이미 사용된 토큰입니다"),

    // === 사용자 관련 ===
    USER_NOT_FOUND(404, "user001", "사용자를 찾을 수 없습니다"),
    USER_BANNED(403, "user002", "영구 정지된 사용자입니다"),
    INVALID_USER_STATUS(400, "user003", "유효하지 않은 사용자 상태입니다"),

    // === 관리자 관련 ===
    ADMIN_NOT_FOUND(404, "admin001", "관리자를 찾을 수 없습니다"),
    ADMIN_PASSWORD_MISMATCH(401, "admin002", "비밀번호가 일치하지 않습니다"),
    INVALID_REVIEW_STATUS(400, "admin004", "심사 대상 상태가 아닙니다"),

    // === 프로필 관련 ===
    PROFILE_NOT_FOUND(404, "profile002", "프로필이 존재하지 않습니다"),
    PROFILE_STATE_INCONSISTENT(500, "profile006", "사용자 상태와 프로필 데이터가 일치하지 않습니다"),
    PENDING_PROFILE_NOT_FOUND(404, "profile007", "승인/반려할 Pending 프로필이 없습니다"),
    PROFILE_ALREADY_UNDER_REVIEW(400, "profile009", "이미 심사 중인 프로필이 있습니다"),

    // === 이미지 관련 ===
    IMAGE_NOT_FOUND(404, "image001", "이미지를 찾을 수 없습니다"),
    TOO_MANY_IMAGES(400, "image002", "업로드 가능한 이미지 수를 초과했습니다"),
    FILE_TOO_LARGE(400, "image003", "파일 크기가 10MB를 초과할 수 없습니다"),
    UNSUPPORTED_FILE_TYPE(400, "image005", "지원하지 않는 파일 형식입니다. JPEG, PNG, GIF, WEBP 형식만 업로드 가능합니다"),
    FILE_EXTENSION_MISMATCH(400, "image006", "파일 확장자와 파일 형식이 일치하지 않습니다"),
    INVALID_DISPLAY_ORDER(400, "image007", "displayOrder는 1 이상이어야 합니다"),

    // === S3 관련 ===
    S3_UPLOAD_FAILED(500, "s3001", "S3 업로드에 실패했습니다"),
    S3_DELETE_FAILED(500, "s3002", "S3 파일 삭제에 실패했습니다"),
    INVALID_S3_URL(400, "s3003", "유효하지 않은 S3 URL입니다"),

    // === 검증 관련 ===
    VALIDATION_FAILED(400, "valid001", "입력값 검증에 실패했습니다"),

    // === 커뮤니티 관련 ===
    COMMUNITY_ACCESS_DENIED(403, "community001", "커뮤니티 기능은 승인된 사용자만 이용할 수 있습니다"),

    // === 게시판 관련 ===
    POST_NOT_FOUND(404, "post001", "게시글을 찾을 수 없습니다"),
    POST_ALREADY_DELETED(400, "post002", "이미 삭제된 게시글입니다"),
    POST_AUTHOR_MISMATCH(403, "post003", "작성자만 삭제할 수 있습니다"),
    CATEGORY_ACCESS_DENIED(403, "post004", "해당 카테고리에 접근할 수 없습니다"),

    // === 댓글 관련 ===
    COMMENT_NOT_FOUND(404, "comment001", "댓글을 찾을 수 없습니다"),
    COMMENT_ALREADY_DELETED(400, "comment002", "이미 삭제된 댓글입니다"),
    COMMENT_AUTHOR_MISMATCH(403, "comment003", "작성자만 삭제할 수 있습니다"),
    REPLY_TO_REPLY_NOT_ALLOWED(400, "comment004", "대댓글에는 답글을 작성할 수 없습니다"),

    // === 닉네임 생성 관련 ===
    NICKNAME_GENERATION_FAILED(500, "nickname001", "닉네임 생성에 실패했습니다"),

    // === 알림 관련 ===
    NOTIFICATION_NOT_FOUND(404, "notification001", "알림을 찾을 수 없습니다");

    private final int httpStatusCode;
    private final String code;
    private final String message;

    public ProblemDetail toProblemDetail(String requestUri) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(httpStatusCode),
                message
        );

        // Problem Details 필드 설정
        problemDetail.setInstance(URI.create(requestUri));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return problemDetail;
    }
}
