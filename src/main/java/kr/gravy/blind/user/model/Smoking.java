package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 흡연 여부
 */
@Getter
public enum Smoking {
    NON_SMOKER("비흡연"),
    OCCASIONALLY("가끔"),
    SMOKER("흡연");

    private final String description;

    Smoking(String description) {
        this.description = description;
    }
}
