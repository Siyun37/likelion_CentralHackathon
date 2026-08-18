package com.mcm.craft.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateSessionRequest(
        @Schema(example = "mobile") String deviceType,
        @Schema(example = "390") Integer viewportW,
        @Schema(example = "844") Integer viewportH
) {
}
