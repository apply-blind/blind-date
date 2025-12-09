package kr.gravy.blind.admin.initializer;

import kr.gravy.blind.admin.entity.Admin;
import kr.gravy.blind.admin.repository.AdminRepository;
import kr.gravy.blind.auth.model.Grade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")  // local 프로필에서만 빈 생성
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    // 초기 관리자 계정 정보
    private static final String DEFAULT_USERNAME = "jackson";
    private static final String DEFAULT_PASSWORD = "blinddate";

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.findByUsername(DEFAULT_USERNAME).isPresent()) {
            log.info("초기 관리자 계정이 이미 존재합니다: {}", DEFAULT_USERNAME);
            return;
        }

        // 관리자 계정 생성
        Admin admin = Admin.create(
                DEFAULT_USERNAME,
                passwordEncoder.encode(DEFAULT_PASSWORD)
        );

        adminRepository.save(admin);

        log.info("=".repeat(60));
        log.info("초기 관리자 계정 생성 완료!");
        log.info("Username: {}", DEFAULT_USERNAME);
        log.info("Password: {}", DEFAULT_PASSWORD);
        log.info("Grade: {}", Grade.ADMIN);
        log.info("주의: 이 정보는 local 프로필에서만 표시됩니다.");
        log.info("=".repeat(60));
    }
}
