package com.mcm.craft.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record EventDto(
        @NotBlank @Schema(example = "evt-42") String eventId,
        @NotBlank @Schema(example = "product_view") String eventType,
        @NotNull @Schema(example = "cus_0001") String customerId,
        @Schema(example = "2026-08-18T10:00:00Z") Instant timestamp,
        @Schema(example = "int-42") String interactionId,
        @Schema(example = "{\"product_id\": 1, \"duration_sec\": 12}") Map<String, Object> meta
) {
}
