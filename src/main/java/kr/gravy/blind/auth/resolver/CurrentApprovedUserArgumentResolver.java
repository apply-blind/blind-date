package kr.gravy.blind.auth.resolver;

import kr.gravy.blind.auth.annotation.CurrentApprovedUser;
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

import static kr.gravy.blind.common.exception.Status.*;

@Component
@RequiredArgsConstructor
public class CurrentApprovedUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationHelper authenticationHelper;
    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentApprovedUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        User user = authenticationHelper.findAuthenticatedEntity(
                userRepository::findByPublicId,
                USER_NOT_FOUND
        );

        if (user.isBanned()) {
            throw new BlindException(USER_BANNED);
        }

        if (!user.hasAccessToCommunity()) {
            throw new BlindException(COMMUNITY_ACCESS_DENIED);
        }

        return user;
    }
}
