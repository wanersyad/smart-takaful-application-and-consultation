package com.muqmeen.takaful.service.chat;

import java.util.List;

public record GeminiContent(String role, List<GeminiPart> parts) {
}
