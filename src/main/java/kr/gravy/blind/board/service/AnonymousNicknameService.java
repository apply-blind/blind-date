package kr.gravy.blind.board.service;

import kr.gravy.blind.board.entity.Post;
import kr.gravy.blind.board.utils.AnonymousNicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnonymousNicknameService {

    private final AnonymousNicknameGenerator nicknameGenerator;

    public String getNicknameFor(Post post, Long userId) {
        if (post.isAuthor(userId)) {
            return post.getAnonymousNickname();
        } else {
            return nicknameGenerator.generateConsistentNickname(userId, post.getId());
        }
    }
}
