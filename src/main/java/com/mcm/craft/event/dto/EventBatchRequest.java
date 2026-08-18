package com.mcm.craft.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record EventBatchRequest(
        @NotNull @Schema(example = "c6b5508a-b6ca-4ae0-980a-2e46a7fd4af4") UUID customerId,
        @NotEmpty List<@Valid EventDto> events
) {
}
