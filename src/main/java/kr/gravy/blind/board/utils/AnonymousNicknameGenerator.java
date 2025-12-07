package kr.gravy.blind.board.utils;

import kr.gravy.blind.common.exception.BlindException;
import kr.gravy.blind.configuration.properties.HmacProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import static kr.gravy.blind.common.exception.Status.NICKNAME_GENERATION_FAILED;

@Component
public class AnonymousNicknameGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HMAC_INPUT_DELIMITER = ":";
    private static final int MINIMUM_HMAC_SECRET_BYTES = 32;
    private static final String NICKNAME_SEPARATOR = " ";

    private final String hmacSecret;

    public AnonymousNicknameGenerator(HmacProperties hmacProperties) {
        this.hmacSecret = hmacProperties.secret();

        byte[] secretBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_HMAC_SECRET_BYTES) {
            throw new IllegalStateException(
                    "HMAC_SECRET는 최소 " + MINIMUM_HMAC_SECRET_BYTES + " bytes 이상이어야 합니다. " +
                            "(현재: " + secretBytes.length + " bytes)"
            );
        }

        try {
            Mac.getInstance(HMAC_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "HMAC 알고리즘이 지원되지 않습니다: " + HMAC_ALGORITHM, e
            );
        }
    }

    private static final String[] ADJECTIVES = {
            "순수한", "사랑스러운", "용감한", "지혜로운", "활발한",
            "귀여운", "멋진", "상냥한", "친절한", "재치있는",
            "명랑한", "씩씩한", "당당한", "유쾌한", "발랄한",
            "차분한", "온화한", "단정한", "깔끔한", "성실한",
            "똑똑한", "영리한", "슬기로운", "현명한", "총명한",
            "열정적인", "적극적인", "긍정적인", "낙천적인", "밝은",
            "따뜻한", "다정한", "포근한", "시원한", "상쾌한",
            "깨끗한", "맑은", "투명한", "빛나는", "반짝이는",
            "환한", "화사한", "화려한", "소중한", "특별한",
            "훌륭한", "대단한", "놀라운", "신비한", "매력적인"
    };

    private static final String[] NOUNS = {
            "둘리", "뭉치", "토토", "마이콜", "또치",
            "피카츄", "라이츄", "파이리", "꼬부기", "버터플",
            "짱구", "철수", "유리", "훈이", "맹구",
            "스누피", "찰리", "루시", "라이너스", "샐리",
            "도라에몽", "진구", "퉁퉁이", "비실이", "이슬이",
            "코난", "란", "회색", "소노코", "고로",
            "나루토", "사스케", "사쿠라", "카카시", "가아라",
            "루피", "조로", "나미", "우솝", "상디",
            "포켓몬", "디지몬", "요괴", "요정", "닌자",
            "하츄핑", "티니핑", "샌드핑", "아자핑", "차차핑"
    };

    private static final int ADJECTIVES_COUNT = ADJECTIVES.length;
    private static final int NOUNS_COUNT = NOUNS.length;

    public String generateConsistentNickname(Long userId, Long postId) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(keySpec);

            String input = userId + HMAC_INPUT_DELIMITER + postId;
            byte[] hmacBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.wrap(hmacBytes);
            long seed = buffer.getLong();

            Random random = new Random(seed);
            String adjective = ADJECTIVES[random.nextInt(ADJECTIVES_COUNT)];
            String noun = NOUNS[random.nextInt(NOUNS_COUNT)];
            return adjective + NICKNAME_SEPARATOR + noun;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BlindException(NICKNAME_GENERATION_FAILED, e);
        }
    }
}
