package kr.gravy.blind.auth.oauth;

/**
 * 새로운 소셜 로그인 제공자 추가 시 이 enum에 추가
 */
public enum OAuth2Provider {
    KAKAO("kakao");

    private final String registrationId;

    OAuth2Provider(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getRegistrationId() {
        return registrationId;
    }
}
