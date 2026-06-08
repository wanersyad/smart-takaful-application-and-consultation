package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByApplication(ConsultationApplication application);
}
