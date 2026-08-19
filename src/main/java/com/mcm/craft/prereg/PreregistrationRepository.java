package com.mcm.craft.prereg;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PreregistrationRepository extends JpaRepository<Preregistration, Long> {

    boolean existsByCustomerIdAndProductId(String customerId, Long productId);
}
