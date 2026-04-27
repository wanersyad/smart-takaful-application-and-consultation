package com.muqmeen.takaful.service.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeminiRequest(
        @JsonProperty("system_instruction") GeminiContent systemInstruction,
        List<GeminiContent> contents
) {
}
