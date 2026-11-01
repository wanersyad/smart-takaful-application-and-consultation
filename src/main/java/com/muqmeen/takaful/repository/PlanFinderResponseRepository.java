package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.ContactInquiry;
import com.muqmeen.takaful.domain.PlanFinderResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanFinderResponseRepository extends JpaRepository<PlanFinderResponse, Long> {

    Optional<PlanFinderResponse> findByToken(String token);

    Optional<PlanFinderResponse> findFirstByApplicationOrderByCreatedAtDesc(ConsultationApplication application);

    Optional<PlanFinderResponse> findFirstByInquiryOrderByCreatedAtDesc(ContactInquiry inquiry);
}
