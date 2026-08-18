package com.mcm.craft.session.dto;

import com.mcm.craft.session.Customer;

import java.util.UUID;

public record CreateSessionResponse(
        UUID customerId
) {

    public static CreateSessionResponse from(Customer customer) {
        return new CreateSessionResponse(customer.getId());
    }
}
