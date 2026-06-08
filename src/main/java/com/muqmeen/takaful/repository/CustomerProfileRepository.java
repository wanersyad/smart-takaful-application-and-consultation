package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByCustomer(Customer customer);
}
