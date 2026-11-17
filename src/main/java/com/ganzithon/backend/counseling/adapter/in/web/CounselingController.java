package com.ganzithon.backend.counseling.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.counseling.application.port.in.CounselingUseCase;
import com.ganzithon.backend.counseling.application.port.in.CounselingUseCase.EmotionCard;
import com.ganzithon.backend.counseling.domain.CrisisHotline;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * F-01·02·03·07·09 상담 API. 와이어프레임 흐름:
 * 세션 시작(첫 메시지) → messages → (nextInput=EMOTION_CARD) emotion → messages → (nextInput=POST_INTENSITY) complete.
 */
@RestController
@RequestMapping("/api/v1/counseling/sessions")
public class CounselingController {
    private final CounselingUseCase counseling;

    public CounselingController(CounselingUseCase counseling) {
        this.counseling = counseling;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<TurnData> start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartRequest request) {
        return ApiEnvelope.success(TurnData.from(counseling.start(userId(jwt), request.message(),
                request.malangiSessionId())));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiEnvelope<TurnData> send(@AuthenticationPrincipal Jwt jwt, @PathVariable long sessionId,
            @Valid @RequestBody MessageRequest request) {
        return ApiEnvelope.success(TurnData.from(counseling.send(userId(jwt), sessionId, request.message())));
    }

    @PostMapping("/{sessionId}/emotion")
    public ApiEnvelope<TurnData> emotion(@AuthenticationPrincipal Jwt jwt, @PathVariable long sessionId,
            @Valid @RequestBody EmotionRequest request) {
        return ApiEnvelope.success(TurnData.from(counseling.submitEmotion(userId(jwt), sessionId,
                new EmotionCard(request.primaryEmotion(), request.secondaryEmotion(), request.preIntensity(),
                        request.triggerFactor()))));
    }

    @PostMapping("/{sessionId}/complete")
    public ApiEnvelope<CompletionData> complete(@AuthenticationPrincipal Jwt jwt, @PathVariable long sessionId,
            @Valid @RequestBody CompleteRequest request) {
        EmotionCard emotion = request.primaryEmotion() == null ? null
                : new EmotionCard(request.primaryEmotion(), request.secondaryEmotion(), request.preIntensity(), null);
        return ApiEnvelope.success(CompletionData.from(counseling.complete(userId(jwt), sessionId,
                request.postIntensity(), emotion)));
    }

    @PostMapping("/{sessionId}/abort")
    public ApiEnvelope<AbortData> abort(@AuthenticationPrincipal Jwt jwt, @PathVariable long sessionId) {
        var aborted = counseling.abort(userId(jwt), sessionId);
        return ApiEnvelope.success(new AbortData(aborted.sessionId(), aborted.status().name(),
                aborted.currentStep().name(), "상담이 중단되었습니다. 언제든 다시 대화를 시작하실 수 있어요."));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long sessionId) {
        counseling.delete(userId(jwt), sessionId);
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public record StartRequest(@NotBlank @Size(max = 2000) String message, Long malangiSessionId) {}

    public record MessageRequest(@NotBlank @Size(max = 2000) String message) {}

    public record EmotionRequest(@NotBlank @Size(max = 20) String primaryEmotion, @Size(max = 20) String secondaryEmotion,
            @NotNull @Min(0) @Max(10) Integer preIntensity, @Size(max = 500) String triggerFactor) {}

    /** primaryEmotion·preIntensity는 감정 기록 카드 전에 상담을 마쳤을 때만 보낸다. */
    public record CompleteRequest(@NotNull @Min(0) @Max(10) Integer postIntensity, @Size(max = 20) String primaryEmotion,
            @Size(max = 20) String secondaryEmotion, @Min(0) @Max(10) Integer preIntensity) {}

    public record HotlineData(String name, String tel, String available) {
        static HotlineData from(CrisisHotline hotline) {
            return new HotlineData(hotline.displayName(), hotline.phoneNumber(), hotline.operatingHours());
        }
    }

    public record EmotionCardData(String triggerFactor, List<String> primaryEmotions, List<String> secondaryEmotions) {}

    public record TurnData(long sessionId, String status, String currentStep, String aiMessage, boolean riskDetected,
            List<HotlineData> emergencyHotlines, List<String> replyOptions, String nextInput,
            EmotionCardData emotionCard) {
        static TurnData from(CounselingUseCase.Turn turn) {
            var card = turn.emotionCard();
            return new TurnData(turn.sessionId(), turn.status().name(), turn.currentStep().name(), turn.aiMessage(),
                    turn.riskDetected(), turn.emergencyHotlines().stream().map(HotlineData::from).toList(),
                    turn.replyOptions(), turn.nextInput().name(), card == null ? null
                    : new EmotionCardData(card.triggerFactor(), card.primaryEmotions(), card.secondaryEmotions()));
        }
    }

    public record MalangiData(String type, String name, boolean isNewlyUnlocked, String reason) {}

    public record CompletionData(long resultId, long sessionId, String status, String aiMessage, String primaryEmotion,
            String secondaryEmotion, Integer preIntensity, int postIntensity, Integer intensityChange, String summary,
            String keySentence, Map<String, String> record, MalangiData recommendedMalangi) {
        static CompletionData from(CounselingUseCase.Completion c) {
            return new CompletionData(c.resultId(), c.sessionId(), c.status().name(), c.aiMessage(), c.primaryEmotion(),
                    c.secondaryEmotion(), c.preIntensity(), c.postIntensity(), c.intensityChange(), c.summary(),
                    c.keySentence(), c.record(), new MalangiData(c.recommendedMalangi().name(),
                    c.recommendedMalangi().displayName(), c.newlyUnlocked(), c.malangiReason()));
        }
    }

    public record AbortData(long sessionId, String status, String currentStep, String message) {}
}
