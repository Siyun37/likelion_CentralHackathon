package com.mcm.craft.prereg;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PreregistrationRepository extends JpaRepository<Preregistration, Long> {

    boolean existsByCustomerIdAndProductId(UUID customerId, Long productId);
}
