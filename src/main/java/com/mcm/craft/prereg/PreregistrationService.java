package com.mcm.craft.prereg;

import com.mcm.craft.prereg.dto.PreregistrationRequest;
import com.mcm.craft.prereg.dto.PreregistrationResponse;
import com.mcm.craft.prereg.exception.DuplicatePreregistrationException;
import com.mcm.craft.session.Customer;
import com.mcm.craft.session.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreregistrationService {

    private final PreregistrationRepository preregistrationRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public PreregistrationResponse register(PreregistrationRequest request) {
        if (preregistrationRepository.existsByCustomerIdAndProductId(request.customerId(), request.productId())) {
            throw new DuplicatePreregistrationException(request.customerId(), request.productId());
        }

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        LocalDateTime consentAt = Boolean.TRUE.equals(request.consent()) ? LocalDateTime.now() : null;

        Preregistration preregistration;
        try {
            preregistration = preregistrationRepository.save(
                    new Preregistration(request.customerId(), request.productId(), request.name(), request.phone(), consentAt, request.color(), request.size())
            );
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePreregistrationException(request.customerId(), request.productId());
        }

        customer.markIdentified();

        return PreregistrationResponse.from(preregistration);
    }
}
