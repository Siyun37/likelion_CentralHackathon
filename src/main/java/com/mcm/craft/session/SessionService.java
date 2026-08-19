package com.mcm.craft.session;

import com.mcm.craft.session.dto.CreateSessionResponse;
import com.mcm.craft.session.dto.UpdateProfileRequest;
import com.mcm.craft.session.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private static final String REAL_CUSTOMER_ID_RANGE_PREFIX = "cus_0";
    private static final String CUSTOMER_ID_FORMAT = "cus_%04d";

    private final CustomerRepository customerRepository;

    @Transactional
    public CreateSessionResponse createSession() {
        Customer customer = customerRepository.save(Customer.create(nextCustomerId()));
        return CreateSessionResponse.from(customer);
    }

    private String nextCustomerId() {
        long nextSeq = customerRepository.countByIdStartingWith(REAL_CUSTOMER_ID_RANGE_PREFIX) + 1;
        return CUSTOMER_ID_FORMAT.formatted(nextSeq);
    }

    @Transactional
    public void updateProfile(String customerId, UpdateProfileRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.updateProfile(request.gender(), request.age(), request.height(), request.weight(), request.bodyType());
    }
}
