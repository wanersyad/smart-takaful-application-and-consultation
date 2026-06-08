package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.repository.ConsultationApplicationRepository;
import com.muqmeen.takaful.repository.CustomerRepository;
import com.muqmeen.takaful.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LandingMetricsService {

    private final CustomerRepository customerRepository;
    private final ConsultationApplicationRepository applicationRepository;
    private final ProductRepository productRepository;

    public LandingMetricsService(CustomerRepository customerRepository,
                                 ConsultationApplicationRepository applicationRepository,
                                 ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.applicationRepository = applicationRepository;
        this.productRepository = productRepository;
    }

    public LandingMetrics current() {
        return new LandingMetrics(
                customerRepository.count(),
                applicationRepository.count(),
                productRepository.countByActiveTrueAndArchivedFalse(),
                applicationRepository.countByStatusIn(List.of(
                        ApplicationStatus.QUOTED,
                        ApplicationStatus.PAYMENT_PENDING,
                        ApplicationStatus.PAID,
                        ApplicationStatus.CLOSED
                ))
        );
    }

    public record LandingMetrics(long customers, long applications, long activeProducts, long quotedOrPaid) {
    }
}
