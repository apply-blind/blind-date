package kr.gravy.blind.board.service;

import kr.gravy.blind.board.dto.CreateCommentDto;
import kr.gravy.blind.board.dto.CreateReplyDto;
import kr.gravy.blind.board.dto.GetCommentsDto;
import kr.gravy.blind.board.dto.ToggleCommentLikeDto;
import kr.gravy.blind.board.entity.Comment;
import kr.gravy.blind.board.entity.CommentLike;
import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.event.CommentAddedEvent;
import kr.gravy.blind.board.event.CommentCreatedEvent;
import kr.gravy.blind.board.event.CommentDeletedEvent;
import kr.gravy.blind.board.event.ReplyCreatedEvent;
import kr.gravy.blind.board.model.CommentStatus;
import kr.gravy.blind.board.repository.CommentLikeRepository;
import kr.gravy.blind.board.repository.CommentRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kr.gravy.blind.common.exception.Status.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("<@([^>]+)>");

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AnonymousNicknameService nicknameService;

    /**
     * @param postPublicId 게시글 publicId
     * @param user         현재 로그인한 사용자
     * @param request      댓글 작성 요청
     * @return CreateCommentDto.Response (commentPublicId)
     */
    @Transactional
    public CreateCommentDto.Response createComment(UUID postPublicId, User user, CreateCommentDto.Request request) {
        Post post = postRepository.findByPublicId(postPublicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new BlindException(POST_ALREADY_DELETED);
        }

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PROFILE_NOT_FOUND));

        String anonymousNickname = nicknameService.getNicknameFor(post, user.getId());

        Comment comment = Comment.createComment(
                post.getId(),
                user.getId(),
                userProfile.getGender(),
                anonymousNickname,
                request.content()
        );
        commentRepository.save(comment);

        postRepository.incrementCommentCount(post.getId());

        // 게시글 작성자에게 1:1 알림 (자기 자신 제외)
        if (!post.getUserId().equals(user.getId())) {
            applicationEventPublisher.publishEvent(
                    CommentCreatedEvent.of(
                            comment.getPublicId(),
                            post.getPublicId(),
                            post.getTitle(),
                            post.getUserId(),  // 게시글 작성자
                            comment.getContent()
                    )
            );
        }

        // 브로드캐스트 알림 (실시간 댓글 목록 업데이트)
        applicationEventPublisher.publishEvent(
                CommentAddedEvent.of(
                        post.getPublicId(),
                        comment.getPublicId()
                )
        );

        return CreateCommentDto.Response.of(comment);
    }

    /**
     * @param commentPublicId 부모 댓글 publicId
     * @param user            현재 로그인한 사용자
     * @param request         대댓글 작성 요청
     * @return CreateReplyDto.Response (replyPublicId)
     */
    @Transactional
    public CreateReplyDto.Response createReply(UUID commentPublicId, User user, CreateReplyDto.Request request) {
        Comment parentComment = commentRepository.findByPublicId(commentPublicId)
                .orElseThrow(() -> new BlindException(COMMENT_NOT_FOUND));

        if (parentComment.isDeleted()) {
            throw new BlindException(COMMENT_ALREADY_DELETED);
        }

        if (parentComment.isReply()) {
            throw new BlindException(REPLY_TO_REPLY_NOT_ALLOWED);
        }

        Post post = postRepository.findById(parentComment.getPostId())
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BlindException(PROFILE_NOT_FOUND));

        String anonymousNickname = nicknameService.getNicknameFor(post, user.getId());

        Comment reply = Comment.createReply(
                post.getId(),
                parentComment.getId(),
                user.getId(),
                userProfile.getGender(),
                anonymousNickname,
                request.content()
        );
        commentRepository.save(reply);

        postRepository.incrementCommentCount(post.getId());

        String mentionedNickname = extractMentionedNickname(request.content());

        // 멘션된 사용자에게 1:1 알림
        if (mentionedNickname != null) {
            Optional<Long> mentionedUserId = commentRepository
                    .findUserIdByPostIdAndNickname(post.getId(), mentionedNickname, CommentStatus.ACTIVE);

            // 자기 자신이 아니면 알림 발송
            if (mentionedUserId.isPresent() && !mentionedUserId.get().equals(user.getId())) {
                applicationEventPublisher.publishEvent(
                        ReplyCreatedEvent.of(
                                reply.getPublicId(),
                                post.getPublicId(),
                                post.getTitle(),
                                mentionedUserId.get(),  // 멘션된 사용자
                                reply.getContent()
                        )
                );
            }
        }

        // 브로드캐스트 알림 (실시간 댓글 목록 업데이트)
        applicationEventPublisher.publishEvent(
                CommentAddedEvent.of(
                        post.getPublicId(),
                        reply.getPublicId()
                )
        );

        return CreateReplyDto.Response.of(reply);
    }

    /**
     * @param postPublicId 게시글 publicId
     * @param user         현재 로그인한 사용자
     * @param pageable     페이징 정보
     * @return GetCommentsDto.PageResponse (계층적 구조)
     */
    public GetCommentsDto.PageResponse getCommentsByPost(UUID postPublicId, User user, Pageable pageable) {
        Post post = postRepository.findByPublicId(postPublicId)
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        Page<Comment> commentPage = commentRepository.findCommentsByPostIdWithReplies(post.getId(), pageable);

        Set<Long> allCommentIds = commentPage.getContent().stream()
                .flatMap(comment -> Stream.concat(
                        Stream.of(comment.getId()),
                        comment.getReplies().stream().map(Comment::getId)
                ))
                .collect(Collectors.toSet());

        Set<Long> likedCommentIds = allCommentIds.isEmpty()
                ? Set.of()
                : commentLikeRepository.findLikedCommentIdsByUserIdAndCommentIds(user.getId(), allCommentIds);

        List<GetCommentsDto.CommentResponse> commentResponses = commentPage.getContent().stream()
                .map(comment -> {
                    boolean isCommentLiked = likedCommentIds.contains(comment.getId());
                    boolean isCommentAuthor = comment.getUserId().equals(user.getId());

                    List<GetCommentsDto.ReplyResponse> replyResponses = comment.getReplies().stream()
                            .map(reply -> {
                                boolean isReplyLiked = likedCommentIds.contains(reply.getId());
                                boolean isReplyAuthor = reply.getUserId().equals(user.getId());
                                return GetCommentsDto.ReplyResponse.of(reply, isReplyLiked, isReplyAuthor);
                            })
                            .toList();

                    return GetCommentsDto.CommentResponse.of(comment, isCommentLiked, isCommentAuthor, replyResponses);
                })
                .toList();

        return GetCommentsDto.PageResponse.of(commentPage, commentResponses);
    }

    /**
     * 댓글 삭제 (SoftDelete)
     *
     * @param commentPublicId 댓글 publicId
     * @param user            현재 로그인한 사용자
     */
    @Transactional
    public void deleteComment(UUID commentPublicId, User user) {
        Comment comment = commentRepository.findByPublicId(commentPublicId)
                .orElseThrow(() -> new BlindException(COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new BlindException(COMMENT_ALREADY_DELETED);
        }

        if (!comment.getUserId().equals(user.getId())) {
            throw new BlindException(COMMENT_AUTHOR_MISMATCH);
        }

        postRepository.decrementCommentCount(comment.getPostId());

        // SoftDelete
        comment.delete();
        commentRepository.save(comment);

        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new BlindException(POST_NOT_FOUND));

        applicationEventPublisher.publishEvent(
                CommentDeletedEvent.of(post.getPublicId(), comment.getPublicId())
        );
    }

    /**
     * 댓글 좋아요 토글
     * - 존재하면 삭제 + likeCount 감소
     * - 없으면 생성 + likeCount 증가
     *
     * @param commentPublicId 댓글 publicId
     * @param user            현재 로그인한 사용자
     * @return ToggleCommentLikeDto.LikeToggleResponse (isLiked, likeCount)
     */
    @Transactional
    public ToggleCommentLikeDto.LikeToggleResponse toggleLike(UUID commentPublicId, User user) {
        Comment comment = commentRepository.findByPublicId(commentPublicId)
                .orElseThrow(() -> new BlindException(COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new BlindException(COMMENT_ALREADY_DELETED);
        }

        Long commentId = comment.getId();
        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId);

        if (exists) {
            commentLikeRepository.deleteByUserIdAndCommentId(user.getId(), commentId);
            commentRepository.decrementLikeCount(commentId);
        } else {
            CommentLike like = CommentLike.create(user.getId(), commentId);
            commentLikeRepository.save(like);
            commentRepository.incrementLikeCount(commentId);
        }

        comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BlindException(COMMENT_NOT_FOUND));

        return ToggleCommentLikeDto.LikeToggleResponse.of(!exists, comment.getLikeCount());
    }

    /**
     * 멘션 파싱
     * - 정규식으로 <@닉네임> 패턴 추출
     *
     * @param content 댓글 내용
     * @return 멘션된 닉네임 (없으면 null)
     */
    private String extractMentionedNickname(String content) {
        Matcher matcher = MENTION_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
