package kr.gravy.blind.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 직업 정보 검증 어노테이션
 * 클래스 레벨에 적용하여 직업 카테고리와 직업명의 유효성을 검증
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JobValidator.class)
public @interface ValidJob {
    String message() default "유효하지 않은 직업입니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
