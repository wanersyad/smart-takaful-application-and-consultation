package com.muqmeen.takaful.service;

import com.muqmeen.takaful.config.ToyyibPayProperties;
import com.muqmeen.takaful.domain.ApplicationStatus;
import com.muqmeen.takaful.domain.Payment;
import com.muqmeen.takaful.domain.Quotation;
import com.muqmeen.takaful.repository.PaymentRepository;
import com.muqmeen.takaful.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final QuotationRepository quotationRepository;
    private final ToyyibPayClient toyyibPayClient;
    private final ToyyibPayProperties toyyibPayProperties;

    public PaymentService(PaymentRepository paymentRepository,
                          QuotationRepository quotationRepository,
                          ToyyibPayClient toyyibPayClient,
                          ToyyibPayProperties toyyibPayProperties) {
        this.paymentRepository = paymentRepository;
        this.quotationRepository = quotationRepository;
        this.toyyibPayClient = toyyibPayClient;
        this.toyyibPayProperties = toyyibPayProperties;
    }

    public PaymentStart prepareQuotationPayment(Quotation quotation) {
        quotation = quotationRepository.findById(quotation.getId())
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found"));
        if (!"PUBLISHED".equals(quotation.getStatus())) {
            throw new IllegalStateException("Only published quotations can be paid.");
        }
        int amountCents = amountCents(quotation.selectedTotal());

        Payment payment = new Payment();
        payment.setQuotation(quotation);
        payment.setCustomer(quotation.getApplication().getCustomer());
        payment.setExternalReferenceNo("MGQ-" + quotation.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setAmountCents(amountCents);

        if (amountCents <= 0) {
            payment.setStatus("PAID");
            quotation.setStatus("PAID");
            quotation.getApplication().setStatus(ApplicationStatus.PAID);
            quotationRepository.save(quotation);
            paymentRepository.save(payment);
            return new PaymentStart(payment, "/applications/" + quotation.getApplication().getId());
        }

        payment.setStatus("PENDING");
        ToyyibPayClient.ToyyibPayBill bill = toyyibPayClient.createBill(quotation, payment);
        payment.setBillCode(bill.billCode());
        quotation.setStatus("PAYMENT_PENDING");
        quotation.getApplication().setStatus(ApplicationStatus.PAYMENT_PENDING);
        quotationRepository.save(quotation);
        paymentRepository.save(payment);
        return new PaymentStart(payment, bill.paymentUrl());
    }

    public Optional<Payment> findByBillCode(String billCode) {
        return Optional.ofNullable(paymentRepository.findByBillCode(billCode));
    }

    public Payment updateMockStatus(String billCode, String statusId) {
        Payment payment = paymentRepository.findByBillCode(billCode);
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found");
        }
        applyStatus(payment, statusId, null, "mock");
        return paymentRepository.save(payment);
    }

    public boolean processCallback(Map<String, String> params) {
        String status = params.getOrDefault("status", params.get("status_id"));
        String orderId = params.get("order_id");
        String refNo = params.get("refno");
        String hash = params.get("hash");

        if (!isValidHash(status, orderId, refNo, hash)) {
            return false;
        }

        Payment payment = paymentRepository.findByExternalReferenceNo(orderId);
        if (payment == null && params.get("billcode") != null) {
            payment = paymentRepository.findByBillCode(params.get("billcode"));
        }
        if (payment == null) {
            return false;
        }

        applyStatus(payment, status, refNo, summarize(params));
        paymentRepository.save(payment);
        return true;
    }

    public boolean isValidHash(String status, String orderId, String refNo, String receivedHash) {
        if (toyyibPayProperties.isMockMode()) {
            return receivedHash != null && receivedHash.equals(expectedHash(status, orderId, refNo));
        }
        if (status == null || orderId == null || refNo == null || receivedHash == null) {
            return false;
        }
        return receivedHash.equalsIgnoreCase(expectedHash(status, orderId, refNo));
    }

    public String expectedHash(String status, String orderId, String refNo) {
        String raw = toyyibPayProperties.getSecretKey()
                + nullToBlank(status)
                + nullToBlank(orderId)
                + nullToBlank(refNo)
                + "ok";
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void applyStatus(Payment payment, String statusId, String providerRefNo, String rawCallback) {
        String normalized = switch (nullToBlank(statusId)) {
            case "1" -> "PAID";
            case "3" -> "FAILED";
            default -> "PENDING";
        };
        payment.setStatus(normalized);
        payment.setProviderRefNo(providerRefNo);
        payment.setRawCallbackSummary(rawCallback);
        Quotation quotation = payment.getQuotation();
        if (quotation != null) {
            quotation.setStatus(normalized);
            if ("PAID".equals(normalized)) {
                quotation.getApplication().setStatus(ApplicationStatus.PAID);
            } else if ("FAILED".equals(normalized)) {
                quotation.getApplication().setStatus(ApplicationStatus.QUOTED);
            } else {
                quotation.getApplication().setStatus(ApplicationStatus.PAYMENT_PENDING);
            }
            quotationRepository.save(quotation);
        }
    }

    private int amountCents(BigDecimal amount) {
        if (amount == null) return 0;
        return amount.multiply(BigDecimal.valueOf(100)).intValue();
    }

    private String summarize(Map<String, String> params) {
        return "status=" + params.get("status")
                + ", billcode=" + params.get("billcode")
                + ", order_id=" + params.get("order_id")
                + ", refno=" + params.get("refno")
                + ", amount=" + params.get("amount");
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    public record PaymentStart(Payment payment, String redirectUrl) {
    }
}
