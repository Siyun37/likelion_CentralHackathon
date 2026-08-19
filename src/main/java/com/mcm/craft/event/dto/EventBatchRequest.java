package com.mcm.craft.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EventBatchRequest(
        @NotNull @Schema(example = "cus_0001") String customerId,
        @NotEmpty List<@Valid EventDto> events
) {
}
