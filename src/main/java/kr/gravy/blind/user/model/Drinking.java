package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 음주 여부
 */
@Getter
public enum Drinking {
    NEVER("전혀 안 함"),
    OCCASIONALLY("가끔"),
    OFTEN("자주"),
    DAILY("매일");

    private final String description;

    Drinking(String description) {
        this.description = description;
    }
}
