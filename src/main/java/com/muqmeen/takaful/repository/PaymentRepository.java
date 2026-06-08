package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.Payment;
import com.muqmeen.takaful.domain.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment findByBillCode(String billCode);

    Payment findByExternalReferenceNo(String externalReferenceNo);

    Payment findByQuotation(Quotation quotation);

    List<Payment> findAllByCustomerOrderByCreatedAtDesc(Customer customer);
}
