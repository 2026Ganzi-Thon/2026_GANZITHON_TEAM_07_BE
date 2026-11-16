package com.ganzithon.backend.counseling.application.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 짧은 답 판정 (process.md: "모르겠어"는 같은 질문을 반복하지 않는다, "그냥 들어줘"는 질문 없이 머문다).
 */
final class UserSignals {
    enum ShortAnswer { DONT_KNOW, NONE }

    private static final Pattern FILLER = Pattern.compile(
            "^(응|음+|어+|아+|흠+|그냥|그래도|사실|솔직히|글쎄|잘|딱히|별로|아직|지금은|그런\\s?(건|거|게))*$");
    private static final Pattern DONT_KNOW = Pattern.compile("(모르겠\\S*|몰라\\S*|모름|글쎄\\S*)$");
    private static final Pattern NONE = Pattern.compile("(없어\\S*|없는데\\S*|없음|없었어\\S*|없네\\S*)$");
    private static final Pattern LISTEN = Pattern.compile(
            "(그냥\\s*들어\\s*줘|들어만\\s*줘|질문\\s*(말고|하지\\s*마|그만)|묻지\\s*(말고|마))");
    private static final Pattern NOISE = Pattern.compile("[.,!?~…ㅠㅜ\\s]+");

    private UserSignals() {}

    static boolean wantsListening(String text) {
        return LISTEN.matcher(text).find();
    }

    static ShortAnswer shortAnswer(String text) {
        String t = NOISE.matcher(text).replaceAll(" ").strip();
        if (matches(DONT_KNOW, t)) return ShortAnswer.DONT_KNOW;
        if (matches(NONE, t)) return ShortAnswer.NONE;
        return null;
    }

    private static boolean matches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return false;
        return FILLER.matcher(text.substring(0, matcher.start()).replace(" ", "")).matches();
    }
}
