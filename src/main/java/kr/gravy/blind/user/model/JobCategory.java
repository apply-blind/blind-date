package kr.gravy.blind.user.model;

import lombok.Getter;

/**
 * 직업 카테고리
 */
@Getter
public enum JobCategory {
    GENERAL("일반"),
    PROFESSIONAL("전문직"),
    MEDICAL("의료직"),
    EDUCATION("교육직"),
    GOVERNMENT("공무원"),
    BUSINESS("사업가"),
    FINANCE("금융직"),
    RESEARCH("연구, 기술직");

    private final String description;

    JobCategory(String description) {
        this.description = description;
    }
}