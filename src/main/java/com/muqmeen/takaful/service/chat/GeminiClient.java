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
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(600);

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

        // The free tier intermittently returns 503 ("high demand") / 429. These are transient, so
        // retry a couple of times with a short backoff before falling back — this is what makes the
        // chatbot feel reliable instead of randomly "unavailable" during Gemini load spikes.
        RestClientException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
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
                lastError = e;
                if (!isTransient(e) || attempt == MAX_ATTEMPTS) {
                    break;
                }
                log.warn("Gemini transient error (attempt {}/{}), retrying: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(RETRY_BACKOFF.toMillis() * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.warn("Gemini API call failed: {}", lastError == null ? "unknown" : lastError.getMessage());
        throw new GeminiException("Gemini API call failed", lastError);
    }

    private static boolean isTransient(RestClientException e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("503") || msg.contains("429")
                || msg.contains("UNAVAILABLE") || msg.contains("overloaded"));
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
