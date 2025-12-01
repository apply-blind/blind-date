package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 이미지 업데이트 타입
 * 프로필 이미지 수정 시 기존 이미지 유지 여부 구분
 */
@Getter
public enum ImageUpdateType {
    EXISTING("기존 이미지 유지"),
    NEW("새 이미지 업로드");

    private final String description;

    ImageUpdateType(String description) {
        this.description = description;
    }
}