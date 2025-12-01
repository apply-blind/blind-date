package kr.gravy.blind.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kr.gravy.blind.user.dto.ImageMetadataDto;
import kr.gravy.blind.user.model.ImageUpdateType;

import java.util.List;

/**
 * 이미지 메타데이터 검증기
 * 비즈니스 규칙:
 * 1. 정확히 6개 슬롯 (null 가능)
 * 2. 슬롯 1,2,3번은 필수 (null 불가)
 * 3. 전체 3~6개 이미지 필요
 * 4. EXISTING 타입: url 필수
 * 5. NEW 타입: index 필수
 */
public class ImageMetadataValidator implements ConstraintValidator<ValidImageMetadata, List<ImageMetadataDto>> {

    private static final int TOTAL_SLOTS = 6;
    private static final int MIN_IMAGES = 3;
    private static final int MAX_IMAGES = 6;
    private static final int[] REQUIRED_SLOTS = {0, 1, 2}; // 1,2,3번 슬롯

    @Override
    public boolean isValid(List<ImageMetadataDto> value, ConstraintValidatorContext context) {
        if (value == null) {
            addViolation(context, "이미지 메타데이터는 필수입니다");
            return false;
        }

        // 1. 정확히 6개 슬롯
        if (value.size() != TOTAL_SLOTS) {
            addViolation(context, String.format("이미지 슬롯은 정확히 %d개여야 합니다 (현재: %d개)", TOTAL_SLOTS, value.size()));
            return false;
        }

        // 2. 필수 슬롯 검증 (1,2,3번)
        for (int requiredIndex : REQUIRED_SLOTS) {
            if (value.get(requiredIndex) == null) {
                addViolation(context, String.format("%d번 슬롯은 필수입니다", requiredIndex + 1));
                return false;
            }
        }

        // 3. non-null 개수 검증 (3~6개)
        long nonNullCount = value.stream().filter(item -> item != null).count();
        if (nonNullCount < MIN_IMAGES || nonNullCount > MAX_IMAGES) {
            addViolation(context, String.format("이미지는 최소 %d개, 최대 %d개 필요합니다 (현재: %d개)",
                MIN_IMAGES, MAX_IMAGES, nonNullCount));
            return false;
        }

        // 4. 각 항목 타입별 필수 필드 검증
        for (int i = 0; i < value.size(); i++) {
            ImageMetadataDto metadata = value.get(i);
            if (metadata == null) continue;  // 4,5,6번 슬롯은 null 가능

            if (metadata.type() == null) {
                addViolation(context, String.format("%d번 슬롯: 타입은 필수입니다", i + 1));
                return false;
            }

            if (metadata.type() == ImageUpdateType.EXISTING) {
                if (metadata.url() == null || metadata.url().isBlank()) {
                    addViolation(context, String.format("%d번 슬롯: EXISTING 타입은 url이 필수입니다", i + 1));
                    return false;
                }
            } else if (metadata.type() == ImageUpdateType.NEW) {
                if (metadata.index() == null) {
                    addViolation(context, String.format("%d번 슬롯: NEW 타입은 index가 필수입니다", i + 1));
                    return false;
                }
            }
        }

        return true;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}