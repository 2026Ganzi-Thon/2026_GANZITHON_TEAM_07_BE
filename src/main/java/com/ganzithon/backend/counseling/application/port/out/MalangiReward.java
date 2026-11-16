package com.ganzithon.backend.counseling.application.port.out;

import com.ganzithon.backend.malangi.domain.MalangiType;

/** 추천 말랑이 자동 해금 (features.md F-04 Unlock on Recommendation). */
public interface MalangiReward {
    /** @return 이번에 새로 해금했으면 true, 이미 해금돼 있었으면 false */
    boolean unlock(long userId, MalangiType type);
}
