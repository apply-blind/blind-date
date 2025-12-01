package kr.gravy.blind.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import kr.gravy.blind.admin.entity.Admin;
import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.configuration.properties.JwtProperties;
import kr.gravy.blind.user.entity.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static kr.gravy.blind.common.exception.Status.INVALID_TOKEN;
import static kr.gravy.blind.common.exception.Status.TOKEN_EXPIRED;

@Component
public class JWTUtil {

    private final Key key;

    private final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    private final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);

    public JWTUtil(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String createAccessToken(User user) {
        return createToken(user, ACCESS_TOKEN_EXPIRATION);
    }

    public String createRefreshToken(User user) {
        return createToken(user, REFRESH_TOKEN_EXPIRATION);
    }

    public String createAccessToken(Admin admin) {
        return createTokenForAdmin(admin, ACCESS_TOKEN_EXPIRATION);
    }

    public String createRefreshToken(Admin admin) {
        return createTokenForAdmin(admin, REFRESH_TOKEN_EXPIRATION);
    }

    private String createToken(User user, Duration duration) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getPublicId().toString())
                .claim("grade", user.getGrade().name())
                .claim("status", user.getStatus().name())
                .issuer("blind")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration)))
                .signWith(key)
                .compact();
    }

    private String createTokenForAdmin(Admin admin, Duration duration) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(admin.getPublicId().toString())
                .claim("grade", admin.getGrade().name())
                .issuer("blind")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration)))
                .signWith(key)
                .compact();
    }

    public Date getExpiration(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    /**
     * JWT 토큰 검증 (서명 및 만료 확인)
     *
     * @param token 검증할 JWT 토큰
     * @throws BlindException JWT 위변조 또는 만료 시 예외 발생
     */
    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new BlindException(TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BlindException(INVALID_TOKEN);
        }
    }

    /**
     * JWT 토큰 검증 및 Claims 추출
     *
     * @param token 검증할 JWT 토큰
     * @return Claims (subject, issuer 등 모든 claim 포함)
     * @throws BlindException JWT 위변조 또는 만료 시 예외 발생
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BlindException(TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BlindException(INVALID_TOKEN);
        }
    }
}
