package com.ganzithon.backend.counseling.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 위기 신호 2중 감지 체계의 1차 서버 필터입니다.
 * 외부 AI를 부르기 전에 사용자 메시지를 키워드·정규식으로 검사합니다.
 *
 * <p>판정 원칙 (docs/emotional-dialogue/safety.md 2장):
 * <ul>
 *   <li>애매한 고위험 표현은 보수적으로 위험 신호로 판정합니다. 예: "죽고 싶어 ㅋㅋ"도 위험입니다.</li>
 *   <li>"피곤해 죽겠다", "배불러 죽을 것 같아" 같은 일상 과장 표현은 고위험 패턴에 넣지 않아 통과시킵니다.</li>
 *   <li>띄어쓰기·문장부호를 지운 뒤 검사하므로 "죽 고 싶 어"처럼 띄워 써도 감지합니다.</li>
 * </ul>
 * 이 필터를 통과한 메시지도 이후 2차 AI 안전 분류를 거쳐야 합니다.
 */
public final class CrisisSignalFilter {

    /** 고위험 단어를 포함하지만 위기 의미가 아닌 표현. 검사 전에 지웁니다. */
    private static final List<String> BENIGN_PHRASES = List.of("자살골");

    private static final String FAMILY_OR_PARTNER =
            "(아빠|엄마|아버지|어머니|부모님?|남친|남자친구|여친|여자친구|남편|아내|오빠|(?<!인)형|누나|언니|선배|선생님|삼촌|이모|고모|친척)";

    private static final Map<CrisisCategory, List<Pattern>> PATTERNS = Map.of(
            CrisisCategory.SELF_HARM, compile(
                    "자살",
                    "자해",
                    "극단적(인)?선택",
                    "죽고(만)?싶",
                    "죽어버리고싶",
                    "죽는게(더)?(나을|낫)",
                    "죽어야겠",
                    "죽을방법|죽는방법",
                    "목숨을?끊",
                    "목을?매(달|고싶|려고|어야|야겠)",
                    "손목을?(긋|그어|그었|그을)",
                    "(옥상|다리|건물|창문|아파트|베란다|한강).{0,8}뛰어내리",
                    "뛰어내리고싶",
                    "한강에?(뛰어|들어가)",
                    "사라지고싶|사라져버리고싶|없어지고싶",
                    "살기싫|살고싶지않|그만살고싶",
                    "사는게의미없|살아서뭐(해|하)|살아갈이유가?없",
                    "(삶|인생)을?(끝내|마감)|생을(끝내|마감)",
                    "세상을?떠나고싶",
                    "영원히잠들고싶",
                    "태어나지말았",
                    "suicide|killmyself|selfharm|wanttodie"),
            CrisisCategory.HARM_TO_OTHERS, compile(
                    "죽여버리",
                    "죽이고싶",
                    "죽일(거|꺼|까)",
                    "죽여야겠",
                    "(칼|흉기)(로|를).{0,6}(찌르|찔러|죽이)",
                    "찔러버리|찔러죽",
                    "패죽|때려죽",
                    "해치고싶|다치게하고싶",
                    "복수(할|하고싶|해버리)",
                    "kill(him|her|them|you)"),
            CrisisCategory.ABUSE, compile(
                    "학대",
                    "가정폭력|데이트폭력",
                    "성폭행|성폭력|성추행|강간",
                    "폭행당|협박당|감금당",
                    FAMILY_OR_PARTNER + "(이|가|께서)?.{0,10}(때려|때리|때렸|폭행)",
                    FAMILY_OR_PARTNER + "(한테|에게)(맞았|맞아|맞고|두들겨|폭행)"));

    private static final Pattern IGNORED_CHARACTERS = Pattern.compile("[\\s\\p{Punct}·…~ㅡ]+");

    public CrisisAssessment assess(String message) {
        if (message == null || message.isBlank()) {
            return CrisisAssessment.safe();
        }
        String text = normalize(message);
        Set<CrisisCategory> detected = EnumSet.noneOf(CrisisCategory.class);
        PATTERNS.forEach((category, patterns) -> {
            if (patterns.stream().anyMatch(pattern -> pattern.matcher(text).find())) {
                detected.add(category);
            }
        });
        return detected.isEmpty() ? CrisisAssessment.safe() : new CrisisAssessment(detected);
    }

    private static String normalize(String message) {
        String text = IGNORED_CHARACTERS.matcher(message.toLowerCase(Locale.ROOT)).replaceAll("");
        for (String phrase : BENIGN_PHRASES) {
            text = text.replace(phrase, "");
        }
        return text;
    }

    private static List<Pattern> compile(String... regexes) {
        return Arrays.stream(regexes).map(Pattern::compile).toList();
    }
}
