package com.mcm.craft.prereg.dto;

import com.mcm.craft.prereg.Preregistration;

import java.time.LocalDateTime;

public record PreregistrationResponse(
        Long preregId,
        LocalDateTime createdAt
) {

    public static PreregistrationResponse from(Preregistration preregistration) {
        return new PreregistrationResponse(preregistration.getId(), preregistration.getCreatedAt());
    }
}
