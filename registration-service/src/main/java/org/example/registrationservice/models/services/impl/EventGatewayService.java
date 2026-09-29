package org.example.registrationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.registrationservice.clients.EventClient;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventGatewayService {
    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService", fallbackMethod = "getEventFallback")
    public EventResponse getEventById(Long eventId) {
        EventResponse event = eventClient.getEventById(eventId);

        if (event == null || event.id() == null || event.ticketPrice() == null){
            throw new EventServiceException("Event service returned invalid data for event " + eventId);
        }
        if (event.availableTickets() < 0) {
            throw new EventServiceException("Event service must have a non-negative number of available tickets for event " + eventId);
        }

        return event;
    }

    public EventResponse getVehicleFallback(Long eventId, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound
                || (throwable instanceof FeignException fe && fe.status() == 404)) {
            throw new EventNotFoundException(eventId);
        }
        if (throwable instanceof EventNotFoundException vehicleNotFoundException) {
            throw vehicleNotFoundException;
        }
        if (throwable instanceof EventServiceException vehicleServiceException) {
            throw vehicleServiceException;
        }

        throw new EventServiceException(
                "Vehicle service is unavailable for vehicle " + eventId,
                throwable
        );
    }
}