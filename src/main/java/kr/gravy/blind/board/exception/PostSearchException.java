package kr.gravy.blind.board.exception;

/**
 * OpenSearch 검색 및 인덱싱 실패 시 발생하는 예외
 */
public class PostSearchException extends RuntimeException {
    public PostSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
