package com.ganzithon.backend.malangi.domain;

public enum MalangiType {
    SLANGI("슬랑이"),
    SQUISHY("스퀴시"),
    WAKPPU("왁뿌볼");

    private final String displayName;

    MalangiType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
