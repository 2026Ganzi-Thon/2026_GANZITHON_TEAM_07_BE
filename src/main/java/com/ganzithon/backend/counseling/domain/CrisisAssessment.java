package com.ganzithon.backend.counseling.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * 메시지 한 건에 대한 위기 신호 판정 결과입니다.
 *
 * @param categories 감지된 위기 범주. 비어 있으면 위기 신호가 없다는 뜻입니다.
 */
public record CrisisAssessment(Set<CrisisCategory> categories) {

    public CrisisAssessment {
        categories = categories.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(categories));
    }

    public static CrisisAssessment safe() {
        return new CrisisAssessment(Set.of());
    }

    public boolean detected() {
        return !categories.isEmpty();
    }
}
