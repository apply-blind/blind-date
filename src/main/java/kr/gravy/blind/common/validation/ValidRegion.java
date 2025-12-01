package kr.gravy.blind.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 한국 행정구역 검증 어노테이션
 * 클래스 레벨에 적용하여 거주지 및 직장지역의 유효성을 검증
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RegionValidator.class)
public @interface ValidRegion {
    String message() default "유효하지 않은 지역입니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
