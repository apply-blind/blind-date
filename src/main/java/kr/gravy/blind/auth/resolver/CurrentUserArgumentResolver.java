package kr.gravy.blind.auth.resolver;

import kr.gravy.blind.auth.annotation.CurrentUser;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.model.UserStatus;
import kr.gravy.blind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static kr.gravy.blind.common.exception.Status.*;

/**
 * @CurrentUser 어노테이션이 붙은 User 파라미터를 자동 주입하는 ArgumentResolver
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BlindException(AUTHENTICATION_FAILED);
        }

        UUID userPublicId = (UUID) authentication.getPrincipal();

        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new BlindException(USER_NOT_FOUND));

        // 🛡️ 방어선: BANNED 사용자 즉시 차단
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BlindException(USER_BANNED);
        }

        return user;
    }
}
