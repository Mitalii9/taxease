package com.taxease.insight.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;

    public GeminiClient(
            RestClient.Builder builder,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    /**
     * Sends a prompt to Gemini 2.0 Flash and returns the generated text.
     * Throws RuntimeException on HTTP or parsing failure — callers should catch and fall back.
     */
    public String generate(String prompt) {
        log.debug("Calling Gemini API — url={} promptLength={}", apiUrl, prompt.length());

        GeminiRequest body = new GeminiRequest(
                List.of(new Content(List.of(new Part(prompt)))),
                new GenerationConfig(0.7, 2048, new ThinkingConfig(0))
        );

        GeminiResponse response = restClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null
                || response.getCandidates() == null
                || response.getCandidates().isEmpty()) {
            log.error("Gemini API returned an empty or null candidates list");
            throw new RuntimeException("Empty response from Gemini API");
        }

        List<Part> parts = response.getCandidates().get(0).getContent().getParts();
        if (parts == null || parts.isEmpty()) {
            log.error("Gemini API candidate contained no parts");
            throw new RuntimeException("No parts in Gemini response candidate");
        }

        String text = parts.get(0).getText();
        log.info("Gemini API call succeeded — responseLength={} chars", text.length());
        return text;
    }

    // ── Request types ────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class GeminiRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class GenerationConfig {
        private double temperature;
        private int maxOutputTokens;
        private ThinkingConfig thinkingConfig;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ThinkingConfig {
        private int thinkingBudget;
    }

    // ── Response types ───────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Candidate {
        private Content content;
    }
}
