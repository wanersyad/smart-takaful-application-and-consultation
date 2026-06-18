package com.muqmeen.takaful.domain;

/**
 * Lifecycle of a general consultation request (someone who wants to speak to an agent without
 * applying for a specific product). Stored as a string on {@link ContactInquiry#getStatus()}.
 */
public enum ConsultationStatus {
    NEW,        // just submitted, not yet actioned
    CONTACTED,  // agent has reached out
    SCHEDULED,  // a consultation session is booked
    COMPLETED,  // consultation done
    CLOSED;     // closed without completing (e.g. not interested, unreachable)

    public static ConsultationStatus fromString(String value) {
        if (value == null) {
            return NEW;
        }
        try {
            return ConsultationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NEW;
        }
    }
}
