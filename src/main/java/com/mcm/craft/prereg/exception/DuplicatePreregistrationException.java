package com.mcm.craft.prereg.exception;

public class DuplicatePreregistrationException extends RuntimeException {

    public DuplicatePreregistrationException(String customerId, Long productId) {
        super("Already preregistered: customerId=%s, productId=%d".formatted(customerId, productId));
    }
}
