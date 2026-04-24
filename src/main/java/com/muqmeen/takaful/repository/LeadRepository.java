package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findAllByOrderByCreatedAtDesc();

    Lead findByBillCode(String billCode);
}
