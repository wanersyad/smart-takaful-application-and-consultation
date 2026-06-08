package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationApplicationRepository extends JpaRepository<ConsultationApplication, Long> {

    List<ConsultationApplication> findAllByOrderByCreatedAtDesc();

    List<ConsultationApplication> findAllByCustomerOrderByCreatedAtDesc(Customer customer);

    Optional<ConsultationApplication> findByIdAndCustomer(Long id, Customer customer);
}
