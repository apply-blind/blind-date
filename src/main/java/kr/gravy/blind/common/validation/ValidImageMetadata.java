package kr.gravy.blind.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 이미지 메타데이터 검증 어노테이션
 * - 정확히 6개 슬롯
 * - 1,2,3번 슬롯 필수
 * - 전체 3~6개 이미지 필수
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageMetadataValidator.class)
@Documented
public @interface ValidImageMetadata {
    String message() default "이미지 메타데이터가 유효하지 않습니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
