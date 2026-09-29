package org.example.registrationservice.models.dto.responses;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RegistrationDetailResponse(
        @JsonAlias("id")
        Long id,

        Long eventId,

        Integer quantity,

        Double ticketPrice,

        Double lineTotal
) {
}
