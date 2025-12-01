package kr.gravy.blind.infrastructure.aws.event;

import java.util.List;

/**
 * 트랜잭션 커밋 후 S3 파일을 삭제하기 위한 이벤트
 * - 트랜잭션과 외부 리소스(S3) 작업 분리
 *
 * @see S3EventListener
 */
public class S3DeleteEvent {

    private final List<String> s3Keys;
    private final S3DeleteSource source;  // 디버깅용: 어디서 발생했는지

    /**
     * S3 삭제 이벤트 생성
     *
     * @param s3Keys 삭제할 S3 객체 키 리스트
     * @param source 이벤트 발생 위치 (S3DeleteSource enum)
     */
    public S3DeleteEvent(List<String> s3Keys, S3DeleteSource source) {
        this.s3Keys = s3Keys != null ? List.copyOf(s3Keys) : List.of();
        this.source = source;
    }

    /**
     * S3 키 리스트 반환
     */
    public List<String> getS3Keys() {
        return s3Keys;
    }

    public S3DeleteSource getSource() {
        return source;
    }

    public int getFileCount() {
        return s3Keys.size();
    }

    @Override
    public String toString() {
        return String.format("S3DeleteEvent[source=%s, count=%d, keys=%s]",
                source, getFileCount(), s3Keys);
    }
}
