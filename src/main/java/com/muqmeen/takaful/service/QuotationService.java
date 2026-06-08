package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.domain.ConsultationApplication;
import com.muqmeen.takaful.domain.Quotation;
import com.muqmeen.takaful.domain.QuotationItem;
import com.muqmeen.takaful.repository.QuotationRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class QuotationService {

    private final QuotationRepository quotationRepository;

    public QuotationService(QuotationRepository quotationRepository) {
        this.quotationRepository = quotationRepository;
    }

    public Optional<Quotation> findById(Long id) {
        return quotationRepository.findById(id).map(this::initializeDetails);
    }

    public Quotation getOrCreate(ConsultationApplication application) {
        return quotationRepository.findByApplication(application).orElseGet(() -> {
            Quotation quotation = new Quotation();
            quotation.setApplication(application);
            return quotationRepository.save(quotation);
        });
    }

    public Quotation save(ConsultationApplication application, String adminNotes, List<QuotationItemInput> items) {
        Quotation quotation = getOrCreate(application);
        quotation.setAdminNotes(adminNotes);
        quotation.getItems().clear();
        int index = 0;
        for (QuotationItemInput input : items) {
            if (input == null || input.itemName() == null || input.itemName().isBlank()) continue;
            QuotationItem item = new QuotationItem();
            item.setQuotation(quotation);
            item.setItemName(input.itemName());
            item.setDescription(input.description());
            item.setAmount(input.amount());
            item.setSelected(input.selected());
            item.setDisplayOrder(index++);
            quotation.getItems().add(item);
        }
        return quotationRepository.save(quotation);
    }

    public Quotation publish(Long quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found"));
        quotation.setStatus("PUBLISHED");
        quotation.setPublishedAt(LocalDateTime.now());
        quotation.getApplication().setStatus(ApplicationStatus.QUOTED);
        return quotationRepository.save(quotation);
    }

    public record QuotationItemInput(String itemName, String description, java.math.BigDecimal amount, boolean selected) {}

    private Quotation initializeDetails(Quotation quotation) {
        Hibernate.initialize(quotation.getApplication());
        Hibernate.initialize(quotation.getApplication().getCustomer());
        Hibernate.initialize(quotation.getItems());
        Hibernate.initialize(quotation.getPayment());
        return quotation;
    }
}
