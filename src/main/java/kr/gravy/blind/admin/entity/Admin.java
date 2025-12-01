package kr.gravy.blind.admin.entity;

import jakarta.persistence.*;
import kr.gravy.blind.auth.model.Grade;
import kr.gravy.blind.common.BaseEntity;
import kr.gravy.blind.common.utils.GeneratorUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 관리자 엔티티
 * 아이디/비밀번호 기반 인증
 */
@Entity
@Table(name = "admins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Grade grade;

    /**
     * 관리자 생성
     */
    public static Admin create(String username, String encodedPassword) {
        return new Admin(null, GeneratorUtil.generatePublicId(), username, encodedPassword, Grade.ADMIN);
    }
}