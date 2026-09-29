package org.example.registrationservice.models.dto.responses;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.example.registrationservice.models.constants.RegistrationStatus;

import java.util.List;

public record RegistrationResponse(
        @JsonAlias("id")
        Long id,

        String participantName,

        String participantEmail,

        Double totalAmount,

        RegistrationStatus status,

        List<RegistrationDetailResponse> details
) {
}
