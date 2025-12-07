package kr.gravy.blind.auth.helper;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.common.exception.Status;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static kr.gravy.blind.common.exception.Status.AUTHENTICATION_FAILED;

/**
 * 인증 정보 추출 및 엔티티 조회 헬퍼
 */
@Component
public class AuthenticationHelper {

    /**
     * SecurityContext에서 인증된 사용자의 publicId 추출
     *
     * @return 인증된 사용자의 publicId (UUID)
     * @throws BlindException 인증 정보가 없거나 유효하지 않을 경우
     */
    public UUID extractAuthenticatedPublicId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BlindException(AUTHENTICATION_FAILED);
        }

        return (UUID) authentication.getPrincipal();
    }

    /**
     * @param finder         publicId로 엔티티를 조회하는 함수 (예: repository::findByPublicId)
     * @param notFoundStatus 엔티티를 찾지 못했을 때 발생시킬 예외 상태 (예: USER_NOT_FOUND, ADMIN_NOT_FOUND)
     * @param <T>            엔티티 타입 (User, Admin 등)
     * @return 조회된 엔티티
     * @throws BlindException 엔티티를 찾지 못했을 경우
     */
    public <T> T findAuthenticatedEntity(Function<UUID, Optional<T>> finder, Status notFoundStatus) {
        UUID publicId = extractAuthenticatedPublicId();
        return finder.apply(publicId)
                .orElseThrow(() -> new BlindException(notFoundStatus));
    }
}
