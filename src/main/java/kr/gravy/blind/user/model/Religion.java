package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 종교
 */
@Getter
public enum Religion {
    NONE("무교"),
    CHRISTIAN("기독교"),
    CATHOLIC("천주교"),
    BUDDHIST("불교"),
    WON_BUDDHIST("원불교"),
    OTHER("기타");

    private final String description;

    Religion(String description) {
        this.description = description;
    }
}
