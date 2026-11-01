package com.muqmeen.takaful.service.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        @JsonProperty("system_instruction") GeminiContent systemInstruction,
        List<GeminiContent> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {

    public GeminiRequest(GeminiContent systemInstruction, List<GeminiContent> contents) {
        this(systemInstruction, contents, null);
    }

    /** Asks Gemini to answer with a JSON document instead of free text. */
    public record GenerationConfig(
            @JsonProperty("responseMimeType") String responseMimeType,
            Double temperature
    ) {
        public static GenerationConfig json() {
            return new GenerationConfig("application/json", 0.3);
        }
    }
}
