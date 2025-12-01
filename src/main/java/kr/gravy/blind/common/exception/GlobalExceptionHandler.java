package kr.gravy.blind.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

import static kr.gravy.blind.common.exception.Status.*;

/**
 * 전역 예외 처리 핸들러
 * RFC 9457 (Problem Details for HTTP APIs) 표준 준수
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BlindException 처리
     */
    @ExceptionHandler(BlindException.class)
    public ResponseEntity<ProblemDetail> handleBlindException(BlindException exception, HttpServletRequest request) {
        log.error("BlindException: {}", exception.getMessage(), exception);

        Status status = exception.getStatus();
        ProblemDetail problemDetail = status.toProblemDetail(request.getRequestURI());

        return ResponseEntity
                .status(status.getHttpStatusCode())
                .body(problemDetail);
    }

    /**
     * Validation 예외 처리 - @Valid 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        log.warn("Validation failed: {}", exception.getMessage());

        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = VALIDATION_FAILED.toProblemDetail(request.getRequestURI());
        problemDetail.setProperty("invalidFields", errors);

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Multipart 예외 처리 - 파일 업로드 관련
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ProblemDetail> handleMultipartException(MultipartException exception, HttpServletRequest request) {
        log.error("MultipartException: {}", exception.getMessage());

        // 파일 개수 초과
        if (exception.getCause() instanceof FileCountLimitExceededException) {
            ProblemDetail problemDetail = TOO_MANY_IMAGES.toProblemDetail(request.getRequestURI());
            return ResponseEntity.badRequest().body(problemDetail);
        }

        // 파일 크기 초과
        if (exception.getMessage() != null && exception.getMessage().contains("maximum upload size")) {
            ProblemDetail problemDetail = FILE_TOO_LARGE.toProblemDetail(request.getRequestURI());
            return ResponseEntity.badRequest().body(problemDetail);
        }

        // 기타 multipart 에러
        log.warn("처리되지 않은 MultipartException: {}", exception.getClass().getSimpleName());
        ProblemDetail problemDetail = BAD_REQUEST.toProblemDetail(request.getRequestURI());
        problemDetail.setDetail("파일 업로드 처리 중 오류가 발생했습니다");

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * IllegalStateException 처리 - 데이터 정합성 오류
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException exception, HttpServletRequest request) {
        log.error("IllegalStateException: {}", exception.getMessage(), exception);

        ProblemDetail problemDetail = PROFILE_STATE_INCONSISTENT.toProblemDetail(request.getRequestURI());
        problemDetail.setDetail(exception.getMessage());

        return ResponseEntity.internalServerError().body(problemDetail);
    }

    /**
     * IllegalArgumentException 처리 - 잘못된 인자
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        log.warn("IllegalArgumentException: {}", exception.getMessage());

        ProblemDetail problemDetail = BAD_REQUEST.toProblemDetail(request.getRequestURI());
        problemDetail.setDetail(exception.getMessage());

        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * RuntimeException 처리 - 예상치 못한 서버 오류
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException exception, HttpServletRequest request) {
        log.error("RuntimeException: {}", exception.getMessage(), exception);

        ProblemDetail problemDetail = INTERNAL_SERVER_ERROR.toProblemDetail(request.getRequestURI());

        return ResponseEntity.internalServerError().body(problemDetail);
    }

}
