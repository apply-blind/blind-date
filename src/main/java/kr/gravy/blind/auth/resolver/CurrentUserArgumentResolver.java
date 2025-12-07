package kr.gravy.blind.auth.resolver;

import kr.gravy.blind.auth.annotation.CurrentUser;
import kr.gravy.blind.auth.helper.AuthenticationHelper;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.user.entity.User;
import kr.gravy.blind.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static kr.gravy.blind.common.exception.Status.USER_BANNED;
import static kr.gravy.blind.common.exception.Status.USER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationHelper authenticationHelper;
    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        User user = authenticationHelper.findAuthenticatedEntity(
                userRepository::findByPublicId,
                USER_NOT_FOUND
        );

        // 🛡️ 방어선: BANNED 사용자 즉시 차단 (User 전용 검증 로직)
        if (user.isBanned()) {
            throw new BlindException(USER_BANNED);
        }

        return user;
    }
}
