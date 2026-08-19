package com.mcm.craft.session.dto;

import com.mcm.craft.session.Customer;

public record CreateSessionResponse(
        String customerId
) {

    public static CreateSessionResponse from(Customer customer) {
        return new CreateSessionResponse(customer.getId());
    }
}
