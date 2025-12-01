package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 성격 (최소 1개, 최대 3개 선택)
 */
@Getter
public enum Personality {
    INTELLIGENT("지적인"),
    CALM("차분한"),
    FUNNY("재미있는"),
    OPTIMISTIC("낙천적인"),
    INTROVERTED("내향적인"),
    EMOTIONAL("감성적인"),
    KIND("상냥한"),
    CUTE("귀여운"),
    PASSIONATE("열정적인"),
    RELIABLE("듬직한"),
    UNIQUE("개성있는"),
    EXTROVERTED("외향적인"),
    SENSIBLE("센스 있는");

    private final String description;

    Personality(String description) {
        this.description = description;
    }
}