package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.ContactInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {

    List<ContactInquiry> findTop5ByOrderByCreatedAtDesc();

    List<ContactInquiry> findTop10ByStatusNotOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
