package com.ganzithon.backend.counseling.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CrisisSignalFilterTests {

    private final CrisisSignalFilter filter = new CrisisSignalFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            "요즘 그냥 죽고 싶다는 생각만 들어",
            "죽고싶어",
            "죽 고 싶 어",
            "죽고 싶어 ㅋㅋ",
            "자살하고 싶다는 생각이 계속 나",
            "어제 또 자해했어",
            "극단적인 선택을 할까 봐 무서워",
            "그냥 사라지고 싶어",
            "더 이상 살기 싫어",
            "살아서 뭐해",
            "옥상에서 뛰어내리고 싶다",
            "손목을 그었어",
            "내 삶을 끝내고 싶어",
            "태어나지 말았어야 했어",
            "I want to die"
    })
    void detectsSelfHarmSignals(String message) {
        CrisisAssessment assessment = filter.assess(message);

        assertThat(assessment.detected()).isTrue();
        assertThat(assessment.categories()).contains(CrisisCategory.SELF_HARM);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "그 사람 진짜 죽여버리고 싶어",
            "팀장을 죽이고 싶다",
            "칼로 찔러버리고 싶어",
            "반드시 복수할 거야"
    })
    void detectsHarmToOthersSignals(String message) {
        CrisisAssessment assessment = filter.assess(message);

        assertThat(assessment.detected()).isTrue();
        assertThat(assessment.categories()).contains(CrisisCategory.HARM_TO_OTHERS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "아빠가 술 마시면 나를 때려",
            "엄마한테 맞았어",
            "남자친구가 폭행했어",
            "가정폭력 때문에 집에 가기 무서워",
            "성추행을 당했어"
    })
    void detectsAbuseSignals(String message) {
        CrisisAssessment assessment = filter.assess(message);

        assertThat(assessment.detected()).isTrue();
        assertThat(assessment.categories()).contains(CrisisCategory.ABUSE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "과제 때문에 진짜 죽겠다 ㅋㅋ",
            "오늘 피곤해 죽겠네",
            "배불러서 죽을 것 같아",
            "시험 망해서 죽을 맛이야",
            "웃겨 죽는 줄 알았어",
            "어제 경기에서 자살골 넣었어",
            "인형을 때렸더니 먼지가 났어",
            "팀플 때문에 너무 속상하고 화나",
            "취업 준비가 불안해서 잠이 안 와",
            "형 말이 맞아"
    })
    void allowsEverydayExpressions(String message) {
        CrisisAssessment assessment = filter.assess(message);

        assertThat(assessment.detected()).isFalse();
        assertThat(assessment.categories()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    void treatsBlankMessagesAsSafe(String message) {
        assertThat(filter.assess(message).detected()).isFalse();
    }

    @Test
    void reportsEveryMatchingCategory() {
        CrisisAssessment assessment = filter.assess("아빠가 나를 때려서 그냥 죽고 싶어");

        assertThat(assessment.categories())
                .containsExactlyInAnyOrder(CrisisCategory.SELF_HARM, CrisisCategory.ABUSE);
    }

    @Test
    void providesGuidanceMessageAndHotlines() {
        assertThat(CrisisHotline.GUIDANCE_MESSAGE).startsWith("혼자 감당하지 않아도 괜찮아요.");
        assertThat(CrisisHotline.values())
                .extracting(CrisisHotline::phoneNumber)
                .containsExactly("109", "1577-0199", "1388", "112", "119");
    }
}
