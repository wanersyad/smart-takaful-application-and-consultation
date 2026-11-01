package com.muqmeen.takaful.web;

import com.muqmeen.takaful.service.chat.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final int RATE_LIMIT_MAX_REQUESTS = 6;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final ChatService chatService;
    private final IpRateLimiter rateLimiter = new IpRateLimiter(RATE_LIMIT_MAX_REQUESTS, RATE_LIMIT_WINDOW);

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest http) {
        if (!rateLimiter.allow(http)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("reply",
                            "You're sending messages a bit too fast. Please wait a minute and try again."));
        }

        List<ChatService.ChatTurn> history = request.history() == null
                ? List.of()
                : request.history().stream()
                        .map(t -> new ChatService.ChatTurn(t.role(), t.text()))
                        .toList();

        String reply = chatService.reply(request.message(), history);
        return ResponseEntity.ok(new ChatReply(reply));
    }

    public record ChatRequest(
            @NotBlank @Size(max = 500) String message,
            List<ChatTurnDto> history
    ) {
    }

    public record ChatTurnDto(String role, String text) {
    }

    public record ChatReply(String reply) {
    }
}
