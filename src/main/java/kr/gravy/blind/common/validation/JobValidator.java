package kr.gravy.blind.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import kr.gravy.blind.user.dto.UserProfileDto;
import kr.gravy.blind.user.model.JobCategory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 직업 정보 검증기
 * 직업 카테고리와 직업명 조합이 유효한지 검증
 */
public class JobValidator implements ConstraintValidator<ValidJob, UserProfileDto.Request> {

    /**
     * 직업 카테고리별 직업명 화이트리스트
     * Key: 직업 카테고리, Value: 직업명 목록
     */
    private static final Map<JobCategory, List<String>> VALID_JOBS;

    static {
        Map<JobCategory, List<String>> jobs = new EnumMap<>(JobCategory.class);
        jobs.put(JobCategory.GENERAL, List.of(
                "무직", "학생", "대기업직원", "중견기업직원", "중소기업직원", "벤처기업직원", "소기업직원",
                "공기업직원", "기관, 협회", "외국계 기업", "승무원", "외국어, 변역, 통역", "IT, 웹, 통신",
                "디자인, 미술, 예술", "미용", "미디어", "음악", "레저, 스포츠", "보안, 경호, 안전관리",
                "외식, 식음료", "상담, 고객센터", "공인중개사", "판매직", "프리랜서", "기타"
        ));
        jobs.put(JobCategory.PROFESSIONAL, List.of(
                "세무, 회계, CP", "법률,특허,상표", "법조인"
        ));
        jobs.put(JobCategory.MEDICAL, List.of(
                "의사", "치과의사", "약사", "한의사", "수의사", "간호사", "간호조무사",
                "의료기사", "사무, 원무, 코디", "기타"
        ));
        jobs.put(JobCategory.EDUCATION, List.of(
                "유치원, 보육", "초,중,고 교사", "입시, 보습, 속셈학원", "외국어, 어학원",
                "학습지, 과외", "전문직업, IT강사", "대학교수", "관리,운영,상담", "기타"
        ));
        jobs.put(JobCategory.GOVERNMENT, List.of(
                "고위 공무원", "일반 공무원", "경찰직공무원", "소방직공무원",
                "특수경력직 공무원", "장교,군인,부사관", "기타"
        ));
        jobs.put(JobCategory.BUSINESS, List.of(
                "농수산업", "제조업", "도소매", "숙박업", "외식업",
                "IT, 웹, 통신", "서비스업", "기타"
        ));
        jobs.put(JobCategory.FINANCE, List.of(
                "펀드매니저", "은행원", "금융감독원", "증권사직원", "보험사직원", "기타"
        ));
        jobs.put(JobCategory.RESEARCH, List.of(
                "IT개발 연구원", "과학 연구원", "기계분야 기술자", "전기, 전자 기술자",
                "건축, 건설 기술자", "제조, 생산 기술자", "기술정보수집 연구원",
                "제품개발 연구원", "제품 및 설계 연구", "운송업", "기타"
        ));
        VALID_JOBS = Collections.unmodifiableMap(jobs);
    }

    @Override
    public boolean isValid(UserProfileDto.Request request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        return isValidJob(request.jobCategory(), request.jobTitle());
    }

    /**
     * 직업 카테고리와 직업명 조합이 유효한지 확인
     */
    private boolean isValidJob(JobCategory category, String title) {
        if (category == null || title == null) {
            return false;
        }

        List<String> titles = VALID_JOBS.get(category);
        return titles != null && titles.contains(title);
    }
}
