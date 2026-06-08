package com.muqmeen.takaful.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muqmeen.takaful.config.AppProperties;
import com.muqmeen.takaful.config.ToyyibPayProperties;
import com.muqmeen.takaful.domain.Payment;
import com.muqmeen.takaful.domain.Quotation;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ToyyibPayClient {

    private final ToyyibPayProperties properties;
    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ToyyibPayClient(ToyyibPayProperties properties, AppProperties appProperties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    public ToyyibPayBill createBill(Quotation quotation, Payment payment) {
        if (properties.isMockMode()) {
            String billCode = "MGM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            return new ToyyibPayBill(billCode, paymentUrl(billCode));
        }

        if (!properties.isConfiguredForGateway()) {
            throw new IllegalStateException("ToyyibPay gateway mode requires secret key, category code, and base URL.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("userSecretKey", properties.getSecretKey());
        form.add("categoryCode", properties.getCategoryCode());
        form.add("billName", limit("Muqmeen_Takaful_Quote", 30));
        form.add("billDescription", limit("Published Takaful quotation payment", 100));
        form.add("billPriceSetting", "1");
        form.add("billPayorInfo", "1");
        form.add("billAmount", String.valueOf(payment.getAmountCents()));
        form.add("billReturnUrl", appProperties.getBaseUrl() + "/payment/return");
        form.add("billCallbackUrl", appProperties.getBaseUrl() + "/payment/callback");
        form.add("billExternalReferenceNo", payment.getExternalReferenceNo());
        form.add("billTo", quotation.getApplication().getApplicantFullName());
        form.add("billEmail", quotation.getApplication().getEmail());
        form.add("billPhone", quotation.getApplication().getPhoneNumber());
        form.add("billSplitPayment", "0");
        form.add("billSplitPaymentArgs", "");
        form.add("billPaymentChannel", "0");
        form.add("billContentEmail", "Thank you for completing your Muqmeen Takaful quotation payment.");
        form.add("billChargeToCustomer", "1");

        String response = restClient.post()
                .uri(properties.getBaseUrl() + "/index.php/api/createBill")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        String billCode = parseBillCode(response);
        return new ToyyibPayBill(billCode, paymentUrl(billCode));
    }

    public String paymentUrl(String billCode) {
        if (properties.isMockMode()) {
            return "/payment/mock/" + billCode;
        }
        return properties.getBaseUrl() + "/" + billCode;
    }

    private String parseBillCode(String response) {
        try {
            List<Map<String, String>> rows = objectMapper.readValue(response, new TypeReference<>() {});
            if (rows.isEmpty() || rows.get(0).get("BillCode") == null) {
                throw new IllegalStateException("ToyyibPay did not return a BillCode.");
            }
            return rows.get(0).get("BillCode");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to parse ToyyibPay createBill response.", ex);
        }
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ToyyibPayBill(String billCode, String paymentUrl) {
    }
}
