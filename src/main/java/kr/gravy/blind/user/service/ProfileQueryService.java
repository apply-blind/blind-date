package kr.gravy.blind.user.service;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.infrastructure.aws.S3Service;
import kr.gravy.blind.user.dto.MyInfoDto;
import kr.gravy.blind.user.dto.NicknameCheckDto;
import kr.gravy.blind.user.dto.UserProfileDto;
import kr.gravy.blind.user.entity.*;
import kr.gravy.blind.user.repository.UserImagePendingRepository;
import kr.gravy.blind.user.repository.UserImageRepository;
import kr.gravy.blind.user.repository.UserProfilePendingRepository;
import kr.gravy.blind.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static kr.gravy.blind.common.exception.Status.*;

/**
 * 프로필 조회 서비스
 * 사용자 정보, 프로필 조회 및 닉네임 중복 체크 등 읽기 전용 작업 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfilePendingRepository userProfilePendingRepository;
    private final UserImageRepository userImageRepository;
    private final UserImagePendingRepository userImagePendingRepository;
    private final S3Service s3Service;

    /**
     * 현재 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public MyInfoDto.Response getUserBasicInfo(User user) {
        UserProfile userProfile = userProfileRepository
                .findByUserId(user.getId())
                .orElse(null);

        // 데이터 정합성 검증: APPROVED인데 프로필 없으면 비정상 (Tell, Don't Ask)
        user.validateProfileExists(userProfile);

        return MyInfoDto.Response.of(user, userProfile);
    }

    /**
     * 내 프로필 전체 정보 조회
     * 프로필 수정 시 기존 데이터 로드용
     */
    @Transactional(readOnly = true)
    public UserProfileDto.Response getUserProfileDetails(User user) {
        return switch (user.getStatus()) {
            case APPROVED -> {
                // APPROVED는 반드시 Base 프로필 존재해야 함
                if (!user.hasBaseProfile()) {
                    throw new BlindException(PROFILE_STATE_INCONSISTENT);
                }
                yield getApprovedProfileData(user);
            }

            case UNDER_REVIEW -> {
                // 기존 사용자 수정 중 → 원본 프로필 표시
                if (user.hasBaseProfile()) {
                    yield getApprovedProfileData(user);
                }
                // 최초 제출 심사 중 → Pending 프로필 표시
                yield getPendingProfileData(user);
            }

            case REJECTED -> getPendingProfileData(user);

            default -> throw new BlindException(PROFILE_NOT_FOUND);
        };
    }

    /**
     * 승인된 프로필 데이터 조회
     * APPROVED 및 UNDER_REVIEW(수정 중) 상태에서 사용
     */
    private UserProfileDto.Response getApprovedProfileData(User user) {
        UserProfile userProfile = userProfileRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PROFILE_STATE_INCONSISTENT));

        List<UserImage> userImages = userImageRepository
                .findByUserProfileIdOrderByDisplayOrder(userProfile.getId());

        List<UserProfileDto.Response.ImageInfo> presignedImages = userImages.stream()
                .map(image -> new UserProfileDto.Response.ImageInfo(
                        image.getPublicId(),
                        s3Service.generatePresignedUrl(image.getS3Key()),
                        image.getDisplayOrder()
                ))
                .toList();

        return UserProfileDto.Response.of(userProfile, presignedImages);
    }

    /**
     * Pending 프로필 데이터 조회
     * REJECTED 및 UNDER_REVIEW 상태에서 사용
     */
    private UserProfileDto.Response getPendingProfileData(User user) {
        UserProfilePending pending = userProfilePendingRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PENDING_PROFILE_NOT_FOUND));

        List<UserImagePending> pendingImages = userImagePendingRepository
                .findByUserProfilePendingIdOrderByDisplayOrder(pending.getId());

        List<UserProfileDto.Response.ImageInfo> presignedImages = pendingImages.stream()
                .map(image -> new UserProfileDto.Response.ImageInfo(
                        image.getPublicId(),
                        s3Service.generatePresignedUrl(image.getS3Key()),
                        image.getDisplayOrder()
                ))
                .toList();

        return UserProfileDto.Response.of(pending, presignedImages);
    }

    /**
     * 닉네임 사용 가능 여부 확인
     * 자기 자신을 제외하고 중복 체크
     */
    @Transactional(readOnly = true)
    public NicknameCheckDto.Response checkNickname(String nickname, Long currentUserId) {
        boolean isDuplicate = userProfileRepository
                .existsByNicknameAndUserIdNot(nickname, currentUserId);
        return NicknameCheckDto.Response.of(!isDuplicate);
    }
}
