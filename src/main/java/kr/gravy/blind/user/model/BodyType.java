package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 체형
 * 성별에 따라 허용되는 값이 다름 (애플리케이션 레벨에서 검증)
 * 여성: SLIM, AVERAGE_SLIM, AVERAGE, SLIGHTLY_VOLUMINOUS, GLAMOROUS, CHUBBY
 * 남성: SLIM, SLIM_MUSCULAR, AVERAGE, MUSCULAR, CHUBBY, BULKY
 */
@Getter
public enum BodyType {
    // 공통
    SLIM("마른"),
    AVERAGE("보통"),
    CHUBBY("통통"),

    // 여성 전용
    AVERAGE_SLIM("슬림"),
    SLIGHTLY_VOLUMINOUS("다소 볼륨"),
    GLAMOROUS("글래머"),

    // 남성 전용
    SLIM_MUSCULAR("슬림근육"),
    MUSCULAR("근육질"),
    BULKY("우람");

    private final String description;

    BodyType(String description) {
        this.description = description;
    }
}
