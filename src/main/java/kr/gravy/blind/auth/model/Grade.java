package kr.gravy.blind.auth.model;

public enum Grade {
    USER("사용자"),
    ADMIN("관리자");

    private final String description;

    Grade(String description) {
        this.description = description;
    }
}

