package com.mcm.craft.prereg.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreregistrationRequest(
        @NotNull @Schema(example = "cus_0001") String customerId,
        @NotNull @Schema(example = "1") Long productId,
        @NotBlank @Schema(example = "홍길동") String name,
        @NotBlank @Schema(example = "010-1234-5678") String phone,
        @NotNull @Schema(example = "true") Boolean consent
) {
}
