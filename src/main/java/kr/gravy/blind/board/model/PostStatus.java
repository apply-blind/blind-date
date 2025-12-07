package kr.gravy.blind.board.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostStatus {

    ACTIVE("활성"),
    DELETED("삭제됨");

    private final String displayName;
}
