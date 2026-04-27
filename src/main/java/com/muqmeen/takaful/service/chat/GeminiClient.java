package com.muqmeen.takaful.service.chat;

import com.muqmeen.takaful.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        this.restClient = buildRestClient(properties);
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String generate(String systemInstruction, List<GeminiContent> conversation) {
        if (!isConfigured()) {
            throw new GeminiException("GEMINI_API_KEY is not configured");
        }

        GeminiRequest body = new GeminiRequest(
                new GeminiContent("user", List.of(new GeminiPart(systemInstruction))),
                conversation
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{model}:generateContent")
                            .queryParam("key", properties.getApiKey())
                            .build(properties.getModel()))
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new GeminiException("Gemini returned an empty response");
            }
            GeminiContent first = response.candidates().get(0).content();
            if (first == null || first.parts() == null || first.parts().isEmpty()) {
                throw new GeminiException("Gemini returned no content parts");
            }
            String text = first.parts().get(0).text();
            if (text == null || text.isBlank()) {
                throw new GeminiException("Gemini returned blank text");
            }
            return text.trim();
        } catch (RestClientException e) {
            log.warn("Gemini API call failed: {}", e.getMessage());
            throw new GeminiException("Gemini API call failed", e);
        }
    }

    private static RestClient buildRestClient(GeminiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(properties.getConnectTimeoutMs()).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(properties.getReadTimeoutMs()).toMillis());
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
