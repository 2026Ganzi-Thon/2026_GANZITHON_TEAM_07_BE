package com.ganzithon.backend.counseling.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.counseling.application.port.in.CounselingFailure;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.Calendar;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.ConversationItem;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.MonthlyReport;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.RecordDetail;
import com.ganzithon.backend.counseling.application.port.in.RecordUseCase.RecordPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-08 마음 기록함 API (+ 와이어프레임의 월간 캘린더·돌아보기·테스형 분석 레포트·숨기기·대화 다시 보기). */
@RestController
@RequestMapping("/api/v1/records")
public class RecordController {
    private final RecordUseCase records;

    public RecordController(RecordUseCase records) {
        this.records = records;
    }

    /** 목록 탭(최근순 페이징). from·to(yyyy-MM-dd, 한국 날짜, 둘 다 포함)를 주면 주간 보기. */
    @GetMapping
    public ApiEnvelope<RecordPage> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiEnvelope.success(records.list(userId(jwt), page, size, from, to));
    }

    /** 월간 캘린더와 그 달 돌아보기(자주 느낀 감정). year·month가 없으면 이번 달. */
    @GetMapping("/calendar")
    public ApiEnvelope<Calendar> calendar(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiEnvelope.success(records.calendar(userId(jwt), yearMonth(year, month)));
    }

    /** 테스형의 월간 분석 레포트. year·month가 없으면 이번 달. */
    @GetMapping("/report")
    public ApiEnvelope<MonthlyReport> report(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ApiEnvelope.success(records.monthlyReport(userId(jwt), yearMonth(year, month)));
    }

    @GetMapping("/{resultId}")
    public ApiEnvelope<RecordDetail> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable long resultId) {
        return ApiEnvelope.success(records.detail(userId(jwt), resultId));
    }

    /** 자세히 보기: 그날 대화(사용자·테스형 말, 대화 안 감정 기록 카드). */
    @GetMapping("/{resultId}/messages")
    public ApiEnvelope<MessagesData> messages(@AuthenticationPrincipal Jwt jwt, @PathVariable long resultId) {
        return ApiEnvelope.success(new MessagesData(records.conversation(userId(jwt), resultId)));
    }

    /** 기록 카드의 숨기기 / 다시 보이기. */
    @PatchMapping("/{resultId}/hidden")
    public ApiEnvelope<HiddenData> hide(@AuthenticationPrincipal Jwt jwt, @PathVariable long resultId,
            @Valid @RequestBody HiddenRequest request) {
        records.setHidden(userId(jwt), resultId, request.hidden());
        return ApiEnvelope.success(new HiddenData(resultId, request.hidden()));
    }

    @DeleteMapping("/{resultId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long resultId) {
        records.delete(userId(jwt), resultId);
    }

    private static YearMonth yearMonth(Integer year, Integer month) {
        if (year == null && month == null) return null;
        try {
            if (year == null || month == null) throw new DateTimeException("year와 month는 함께 보낸다");
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        }
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public record MessagesData(List<ConversationItem> messages) {}

    public record HiddenRequest(@NotNull Boolean hidden) {}

    public record HiddenData(long resultId, boolean hidden) {}
}
