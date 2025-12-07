package kr.gravy.blind.board.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ImageUploadStatus {
    NOT_UPLOADED("업로드 대기"),
    UPLOADED("업로드 완료");

    private final String description;
}
