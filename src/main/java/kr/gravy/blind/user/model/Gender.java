package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 성별
 */
@Getter
public enum Gender {
    MALE("남성"),
    FEMALE("여성");

    private final String description;

    Gender(String description) {
        this.description = description;
    }
}
