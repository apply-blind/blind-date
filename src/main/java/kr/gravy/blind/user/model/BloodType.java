package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 혈액형
 */
@Getter
public enum BloodType {
    A("A형"),
    B("B형"),
    O("O형"),
    AB("AB형");

    private final String description;

    BloodType(String description) {
        this.description = description;
    }
}
