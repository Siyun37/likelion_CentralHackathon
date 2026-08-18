package com.mcm.craft.prereg.exception;

import java.util.UUID;

public class DuplicatePreregistrationException extends RuntimeException {

    public DuplicatePreregistrationException(UUID customerId, Long productId) {
        super("Already preregistered: customerId=%s, productId=%d".formatted(customerId, productId));
    }
}
