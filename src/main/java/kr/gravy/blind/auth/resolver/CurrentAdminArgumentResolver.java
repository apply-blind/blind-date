package kr.gravy.blind.auth.resolver;

import kr.gravy.blind.admin.entity.Admin;
import kr.gravy.blind.admin.repository.AdminRepository;
import kr.gravy.blind.auth.annotation.CurrentAdmin;
import kr.gravy.blind.auth.helper.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static kr.gravy.blind.common.exception.Status.ADMIN_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class CurrentAdminArgumentResolver implements HandlerMethodArgumentResolver {

    private final AuthenticationHelper authenticationHelper;
    private final AdminRepository adminRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAdmin.class)
                && Admin.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Admin resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return authenticationHelper.findAuthenticatedEntity(
                adminRepository::findByPublicId,
                ADMIN_NOT_FOUND
        );
    }
}
