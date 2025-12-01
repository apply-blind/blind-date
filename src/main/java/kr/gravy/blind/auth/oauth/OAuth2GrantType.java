package kr.gravy.blind.auth.oauth;

/**
 * OAuth2 Grant Type
 */
public enum OAuth2GrantType {
    AUTHORIZATION_CODE("authorization_code");

    private final String value;

    OAuth2GrantType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
