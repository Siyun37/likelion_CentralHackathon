package com.mcm.craft.session;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    long countByIdStartingWith(String prefix);
}
