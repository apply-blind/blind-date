package kr.gravy.blind.auth.resolver;

import kr.gravy.blind.admin.entity.Admin;
import kr.gravy.blind.admin.repository.AdminRepository;
import kr.gravy.blind.auth.annotation.CurrentAdmin;
import kr.gravy.blind.common.exception.BlindException;
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

import static kr.gravy.blind.common.exception.Status.ADMIN_NOT_FOUND;
import static kr.gravy.blind.common.exception.Status.AUTHENTICATION_FAILED;

/**
 * @CurrentAdmin 어노테이션이 붙은 Admin 파라미터를 자동 주입하는 ArgumentResolver
 */
@Component
@RequiredArgsConstructor
public class CurrentAdminArgumentResolver implements HandlerMethodArgumentResolver {

    private final AdminRepository adminRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAdmin.class)
                && Admin.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Admin resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BlindException(AUTHENTICATION_FAILED);
        }

        UUID adminPublicId = (UUID) authentication.getPrincipal();

        return adminRepository.findByPublicId(adminPublicId)
                .orElseThrow(() -> new BlindException(ADMIN_NOT_FOUND));
    }
}
