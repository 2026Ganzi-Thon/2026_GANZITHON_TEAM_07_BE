package com.ganzithon.backend.malangi.adapter.in.web;

import com.ganzithon.backend.common.adapter.in.web.ApiEnvelope;
import com.ganzithon.backend.malangi.application.port.in.MalangiUseCase;
import com.ganzithon.backend.malangi.application.port.in.MalangiUseCase.Selection;
import com.ganzithon.backend.malangi.domain.OwnedMalangi;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/malangis")
public class MalangiController {
    private final MalangiUseCase malangi;

    public MalangiController(MalangiUseCase malangi) {
        this.malangi = malangi;
    }

    @GetMapping
    public ApiEnvelope<InventoryData> inventory(@AuthenticationPrincipal Jwt jwt) {
        List<MalangiData> items = malangi.inventory(userId(jwt)).stream()
                .map(MalangiData::from)
                .toList();
        return ApiEnvelope.success(new InventoryData(items));
    }

    @PatchMapping("/{malangiType}/select")
    public ApiEnvelope<SelectionData> select(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String malangiType
    ) {
        Selection selection = malangi.select(userId(jwt), malangiType);
        return ApiEnvelope.success(new SelectionData(
                selection.selectedMalangiType().name(), selection.updatedAt()));
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public record InventoryData(List<MalangiData> malangis) {}

    public record MalangiData(String malangiType, String name, boolean isUnlocked, boolean isSelected,
                              int totalPlayTime, LocalDateTime lastInteractedAt) {
        static MalangiData from(OwnedMalangi malangi) {
            return new MalangiData(malangi.type().name(), malangi.type().displayName(), malangi.unlocked(),
                    malangi.selected(), malangi.totalPlayTime(), malangi.lastInteractedAt());
        }
    }

    public record SelectionData(String selectedMalangiType, LocalDateTime updatedAt) {}
}
