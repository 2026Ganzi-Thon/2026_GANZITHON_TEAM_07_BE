package com.ganzithon.backend.counseling.application.port.in;

import com.ganzithon.backend.counseling.domain.CounselingStatus;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.counseling.domain.CrisisHotline;
import com.ganzithon.backend.counseling.domain.NextInput;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.util.List;
import java.util.Map;

/**
 * F-01·02·03·07·09 상담 대화. 와이어프레임 흐름:
 * 첫 메시지로 시작(STEP_01) → 대화 안 감정 기록 카드 → STEP_02~05 → 상담 후 강도 → STEP_06 정리.
 */
public interface CounselingUseCase {
    /** 대화 화면 첫 메시지로 세션을 만들고 STEP_01 대화를 시작한다. */
    Turn start(long userId, String message, Long malangiSessionId);

    /** 사용자 메시지 한 턴. */
    Turn send(long userId, long sessionId, String message);

    /** 감정 기록 카드 제출 (대표·부가 감정, 유발 상황, 상담 전 강도) → STEP_02 첫 질문. */
    Turn submitEmotion(long userId, long sessionId, EmotionCard card);

    /** 상담 후 강도 입력 → STEP_06 정리, 결과 저장, 말랑이 추천·해금. */
    Completion complete(long userId, long sessionId, int postIntensity, EmotionCard emotionIfMissing);

    Aborted abort(long userId, long sessionId);

    void delete(long userId, long sessionId);

    record EmotionCard(String primaryEmotion, String secondaryEmotion, Integer preIntensity, String triggerFactor) {}

    record Turn(long sessionId, CounselingStatus status, CounselingStep currentStep, String aiMessage,
            boolean riskDetected, List<CrisisHotline> emergencyHotlines, List<String> replyOptions,
            NextInput nextInput, EmotionCardPrompt emotionCard) {}

    /** 감정 기록 카드에 미리 채울 값과 고를 수 있는 감정 목록. */
    record EmotionCardPrompt(String triggerFactor, List<String> primaryEmotions, List<String> secondaryEmotions) {}

    record Completion(long resultId, long sessionId, CounselingStatus status, String aiMessage, String primaryEmotion,
            String secondaryEmotion, Integer preIntensity, int postIntensity, Integer intensityChange, String summary,
            String keySentence, Map<String, String> record, MalangiType recommendedMalangi, boolean newlyUnlocked,
            String malangiReason) {}

    record Aborted(long sessionId, CounselingStatus status, CounselingStep currentStep) {}
}
