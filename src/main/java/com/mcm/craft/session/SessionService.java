package com.mcm.craft.session;

import com.mcm.craft.session.dto.CreateSessionResponse;
import com.mcm.craft.session.dto.UpdateProfileRequest;
import com.mcm.craft.session.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CreateSessionResponse createSession() {
        Customer customer = customerRepository.save(Customer.create());
        return CreateSessionResponse.from(customer);
    }

    @Transactional
    public void updateProfile(UUID customerId, UpdateProfileRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.updateProfile(request.gender(), request.age(), request.height(), request.weight(), request.bodyType());
    }
}
