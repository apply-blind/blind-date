package kr.gravy.blind.board.model;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.user.model.Gender;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static kr.gravy.blind.common.exception.Status.CATEGORY_ACCESS_DENIED;

/**
 * 익명 게시판 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum PostCategory {

    FREE_TALK("자유수다"),         // 인기글 선정 가능
    SELF_INTRO("셀소"),           // 최상단 가이드라인만
    MEETUP("벙개"),               // 최상단 가이드라인만
    GENTLEMEN("젠틀맨 라운지"),     // 남성 전용
    LADIES("레이디 라운지");        // 여성 전용

    private final String displayName;


    /**
     * 사용자가 이 카테고리에 접근 가능한지 검증
     *
     * @param userGender 사용자 성별
     * @return 접근 가능 여부
     */
    public boolean canAccess(Gender userGender) {
        return switch (this) {
            case GENTLEMEN -> userGender == Gender.MALE;
            case LADIES -> userGender == Gender.FEMALE;
            default -> true;  // FREE_TALK, SELF_INTRO, MEETUP = 모두 접근 가능
        };
    }

    /**
     * 카테고리 접근 권한 검증
     *
     * @param userGender 사용자 성별
     * @throws BlindException CATEGORY_ACCESS_DENIED
     */
    public void validateAccess(Gender userGender) {
        if (!canAccess(userGender)) {
            throw new BlindException(CATEGORY_ACCESS_DENIED);
        }
    }
}
