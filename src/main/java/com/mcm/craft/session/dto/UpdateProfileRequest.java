package com.mcm.craft.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateProfileRequest(
        @Schema(example = "F") String gender,
        @Schema(example = "28") Integer age,
        @Schema(example = "165") Integer height,
        @Schema(example = "52") Integer weight,
        @Schema(example = "slim") String bodyType
) {
}
