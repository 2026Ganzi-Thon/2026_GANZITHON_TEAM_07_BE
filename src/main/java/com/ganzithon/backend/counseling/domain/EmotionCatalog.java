package com.ganzithon.backend.counseling.domain;

import com.ganzithon.backend.malangi.domain.MalangiType;
import java.util.List;
import java.util.Map;

/**
 * 감정 기록 카드의 감정 목록(와이어프레임 2026-11-14)과 대표 감정 → 말랑이 추천 룰(06번 이슈).
 *
 * <p>06번 이슈 룰: 불안/우울 → 슬랑이, 속상함/슬픔 → 스퀴시, 분노/스트레스 → 왁뿌볼.
 * 와이어프레임에만 있는 감정(불안함, 억울함, 서운함)은 가까운 쪽으로 붙였다 (팀 확인 필요).
 */
public final class EmotionCatalog {
    public static final List<String> PRIMARY = List.of("속상함", "불안함", "분노", "억울함", "슬픔", "서운함");
    public static final List<String> SECONDARY = List.of("답답함", "외로움", "허무함", "서운함", "억울함");

    private static final Map<String, MalangiType> MALANGI = Map.ofEntries(
            Map.entry("불안", MalangiType.SLANGI), Map.entry("불안함", MalangiType.SLANGI),
            Map.entry("우울", MalangiType.SLANGI), Map.entry("초조", MalangiType.SLANGI),
            Map.entry("속상함", MalangiType.SQUISHY), Map.entry("슬픔", MalangiType.SQUISHY),
            Map.entry("서운함", MalangiType.SQUISHY), Map.entry("외로움", MalangiType.SQUISHY),
            Map.entry("분노", MalangiType.WAKPPU), Map.entry("스트레스", MalangiType.WAKPPU),
            Map.entry("억울함", MalangiType.WAKPPU));

    private static final Map<MalangiType, String> DESCRIPTION = Map.of(
            MalangiType.SLANGI, "천천히 만지며 긴장을 푸는 말랑이",
            MalangiType.SQUISHY, "부드럽게 감싸 쥐며 위로받는 말랑이",
            MalangiType.WAKPPU, "강하게 누르고 깨뜨리며 감정을 풀어내는 말랑이");

    private EmotionCatalog() {}

    public static MalangiType malangiFor(String primaryEmotion) {
        if (primaryEmotion == null) return MalangiType.SQUISHY;
        return MALANGI.getOrDefault(primaryEmotion.strip(), MalangiType.SQUISHY);
    }

    public static String describe(MalangiType type) {
        return DESCRIPTION.get(type);
    }
}
