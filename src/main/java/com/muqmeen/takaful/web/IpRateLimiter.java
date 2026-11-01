package com.muqmeen.takaful.web;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window limiter keyed by client IP. Protects endpoints that call the Gemini free tier
 * from being drained by one visitor.
 */
public class IpRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final ConcurrentHashMap<String, Deque<Instant>> hitsByIp = new ConcurrentHashMap<>();

    public IpRateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public boolean allow(HttpServletRequest request) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> hits = hitsByIp.computeIfAbsent(clientIp(request), k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
                hits.pollFirst();
            }
            if (hits.size() >= maxRequests) {
                return false;
            }
            hits.offerLast(now);
            return true;
        }
    }

    static String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return http.getRemoteAddr() == null ? "unknown" : http.getRemoteAddr();
    }
}
