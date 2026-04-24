package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TakafulService {

    private final LeadRepository leadRepository;

    public TakafulService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Lead processNewLead(Lead lead) {
        if (lead.getTipAmount() != null && lead.getTipAmount().compareTo(BigDecimal.ZERO) > 0) {
            String generatedBillCode = "MGM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            lead.setBillCode(generatedBillCode);
            lead.setPaymentStatus("PENDING");
        } else {
            lead.setPaymentStatus("SKIPPED");
        }
        return leadRepository.save(lead);
    }

    public List<Lead> getAllLeadsForAdmin() {
        return leadRepository.findAllByOrderByCreatedAtDesc();
    }

    public void updateLeadStatus(Long id, String status) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        lead.setLeadStatus(status);
        leadRepository.save(lead);
    }

    public void updatePaymentStatus(String billCode, String status) {
        Lead lead = leadRepository.findByBillCode(billCode);
        if (lead != null) {
            lead.setPaymentStatus(status);
            leadRepository.save(lead);
        }
    }
}
