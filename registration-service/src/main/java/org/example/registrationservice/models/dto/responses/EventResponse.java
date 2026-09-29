package org.example.registrationservice.models.dto.responses;

import com.fasterxml.jackson.annotation.JsonAlias;

public record EventResponse(
        @JsonAlias("id")
        Long id,

        String eventName,

        Double ticketPrice,

        Integer availableTickets
) {
}
