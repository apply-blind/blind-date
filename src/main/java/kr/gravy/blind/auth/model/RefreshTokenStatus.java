package kr.gravy.blind.auth.model;

public enum RefreshTokenStatus {
    ACTIVE("사용 가능한 토큰"),
    REVOKED("무효화된 토큰"),
    EXPIRED("만료된 토큰");

    private final String description;

    RefreshTokenStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
