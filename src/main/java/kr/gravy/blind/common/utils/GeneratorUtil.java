package kr.gravy.blind.common.utils;

import com.fasterxml.uuid.Generators;
import lombok.experimental.UtilityClass;

import java.util.Random;
import java.util.UUID;

@UtilityClass
public class GeneratorUtil {

    private static final String NICKNAME_SEPARATOR = " ";

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

    // 배열 길이 캐싱 (성능 최적화)
    private static final int ADJECTIVES_COUNT = ADJECTIVES.length;
    private static final int NOUNS_COUNT = NOUNS.length;

    public static String generateRandomNickname() {
        Random random = new Random();
        String adjective = ADJECTIVES[random.nextInt(ADJECTIVES_COUNT)];
        String noun = NOUNS[random.nextInt(NOUNS_COUNT)];
        return adjective + NICKNAME_SEPARATOR + noun;
    }

    public static UUID generatePublicId() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}
