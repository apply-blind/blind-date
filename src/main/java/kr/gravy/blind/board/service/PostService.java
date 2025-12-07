package kr.gravy.blind.board.service;

import kr.gravy.blind.board.dto.CreatePostDto;
import kr.gravy.blind.board.dto.GetDetailPostDto;
import kr.gravy.blind.board.dto.GetListPostDto;
import kr.gravy.blind.board.dto.TogglePostLikeDto;
import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.entity.PostLike;
import kr.gravy.blind.board.event.PostCreatedEvent;
import kr.gravy.blind.board.event.PostDeletedEvent;
import kr.gravy.blind.board.model.PostCategory;
import kr.gravy.blind.board.model.PostStatus;
import kr.gravy.blind.board.repository.PostImageRepository;
import kr.gravy.blind.board.repository.PostLikeRepository;
import kr.gravy.blind.board.repository.PostRepository;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.entity.UserProfile;
import kr.gravy.blind.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.gravy.blind.common.exception.Status.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostImageService postImageService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * @param user    현재 로그인한 사용자
     * @param request 게시글 생성 요청
     * @return Optional<Response> - 이미지 있으면 Presigned URL 정보, 없으면 empty
     */
    @Transactional
    public Optional<CreatePostDto.Response> createPost(User user, CreatePostDto.Request request) {
        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PROFILE_NOT_FOUND));

        if (!request.category().canAccess(userProfile.getGender())) {
            throw new BlindException(CATEGORY_ACCESS_DENIED);
        }

        Post post = Post.create(
                user.getId(),
                userProfile.getGender(),
                request.category(),
                request.title(),
                request.content()
        );
        postRepository.save(post);

        applicationEventPublisher.publishEvent(PostCreatedEvent.of(post));

        // 5. 이미지가 있으면 Presigned URL 발급
        if (request.imageMetadata() != null) {
            CreatePostDto.PresignedUrlInfo presignedUrlInfo = postImageService.createPresignedUrl(
                    post,
                    request.imageMetadata().filename(),
                    request.imageMetadata().contentType()
            );

            return Optional.of(CreatePostDto.Response.of(post, presignedUrlInfo));
        }

        return Optional.empty();
    }

    @Transactional
    public GetDetailPostDto.Response getDetailPost(UUID publicId, User user) {
        Post post = postRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        Long postId = post.getId();

        postRepository.incrementViewCount(postId);

        // 재조회 (최신 viewCount 반영)
        post = postRepository.findById(postId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        boolean isLiked = postLikeRepository.existsByUserIdAndPostId(user.getId(), postId);
        boolean isAuthor = post.getUserId().equals(user.getId());

        String imageUrl = postImageRepository.findByPostId(post.getId())
                .map(image -> postImageService.getCdnImageUrl(image.getS3Key(), kr.gravy.blind.common.type.ImageSize.MEDIUM))
                .orElse(null);

        return GetDetailPostDto.Response.of(post, isLiked, isAuthor, imageUrl);
    }

    /**
     * S3 이미지 업로드 검증
     * 클라이언트가 S3 업로드 완료 후 호출
     */
    @Transactional
    public void verifyImageUpload(UUID publicId, User user) {
        Post post = postRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        if (!post.getUserId().equals(user.getId())) {
            throw new BlindException(POST_AUTHOR_MISMATCH);
        }

        postImageService.verifyAndCompleteUpload(post.getId());
    }

    public GetListPostDto.PageResponse getPostsByCategory(PostCategory category, User user, Pageable pageable) {

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PROFILE_NOT_FOUND));

        if (!category.canAccess(userProfile.getGender())) {
            throw new BlindException(CATEGORY_ACCESS_DENIED);
        }

        Page<Post> postPage = postRepository.findByCategoryAndStatusWithImages(
                category,
                List.of(PostStatus.ACTIVE, PostStatus.DELETED),
                pageable
        );

        List<String> imageUrlList = postPage.getContent().stream()
                .map(post -> post.getFirstImage()
                        .map(image -> postImageService.getCdnImageUrl(image.getS3Key(), kr.gravy.blind.common.type.ImageSize.THUMBNAIL))
                        .orElse(null))
                .toList();

        return GetListPostDto.PageResponse.of(postPage, imageUrlList);
    }

    public GetListPostDto.PageResponse getHotPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findByIsHotTrueAndStatusWithImages(
                List.of(PostStatus.ACTIVE, PostStatus.DELETED),
                pageable
        );

        List<String> imageUrlList = postPage.getContent().stream()
                .map(post -> post.getFirstImage()  // Post 엔티티의 헬퍼 메서드 사용
                        .map(image -> postImageService.getCdnImageUrl(image.getS3Key(), kr.gravy.blind.common.type.ImageSize.THUMBNAIL))
                        .orElse(null))
                .toList();

        return GetListPostDto.PageResponse.of(postPage, imageUrlList);
    }

    /**
     * 게시글 삭제 (SoftDelete)
     */
    @Transactional
    public void deletePost(UUID publicId, User user) {
        Post post = postRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new BlindException(POST_ALREADY_DELETED);
        }

        if (!post.getUserId().equals(user.getId())) {
            throw new BlindException(POST_AUTHOR_MISMATCH);
        }

        post.delete();

        applicationEventPublisher.publishEvent(
                PostDeletedEvent.of(post.getPublicId(), post.getCategory())
        );
    }

    @Transactional
    public TogglePostLikeDto.LikeToggleResponse toggleLike(UUID publicId, User user) {
        Post post = postRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new BlindException(POST_ALREADY_DELETED);
        }

        Long postId = post.getId();
        boolean exists = postLikeRepository.existsByUserIdAndPostId(user.getId(), postId);

        if (exists) {
            postLikeRepository.deleteByUserIdAndPostId(user.getId(), postId);
            postRepository.decrementLikeCount(postId);
        } else {
            PostLike like = PostLike.create(user.getId(), postId);
            postLikeRepository.save(like);
            postRepository.incrementLikeCount(postId);
        }

        post = postRepository.findById(postId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        return TogglePostLikeDto.LikeToggleResponse.of(!exists, post.getLikeCount());
    }

    /**
     * @param user     현재 로그인한 사용자
     * @param pageable 페이징 정보
     * @return 페이징된 게시글 목록
     */
    public GetListPostDto.PageResponse getMyPosts(User user, Pageable pageable) {
        Page<Post> postPage = postRepository.findByUserIdWithImages(
                user.getId(),
                List.of(PostStatus.ACTIVE, PostStatus.DELETED),
                pageable
        );

        List<String> imageUrlList = postPage.getContent().stream()
                .map(post -> post.getFirstImage()  // Post 엔티티의 헬퍼 메서드 사용
                        .map(image -> postImageService.getCdnImageUrl(image.getS3Key(), kr.gravy.blind.common.type.ImageSize.THUMBNAIL))
                        .orElse(null))
                .toList();

        return GetListPostDto.PageResponse.of(postPage, imageUrlList);
    }
}
