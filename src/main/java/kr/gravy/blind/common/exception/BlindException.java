package kr.gravy.blind.common.exception;

import lombok.Getter;

/**
 * Blind 프로젝트 커스텀 예외
 */
@Getter
public class BlindException extends RuntimeException {
    private final Status status;
    private final int httpStatusCode;
    private final String code;
    private final String message;

    public BlindException(Status status) {
        super(status.name());
        this.status = status;
        this.httpStatusCode = status.getHttpStatusCode();
        this.code = status.getCode();
        this.message = status.getMessage();
    }

    public BlindException(String message) {
        super(Status.BAD_REQUEST.name());
        this.status = Status.BAD_REQUEST;
        this.httpStatusCode = Status.BAD_REQUEST.getHttpStatusCode();
        this.code = Status.BAD_REQUEST.getCode();
        this.message = message;
    }
}
