package org.example.registrationservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationDetailResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.entities.Registration;
import org.example.registrationservice.models.entities.RegistrationDetail;
import org.example.registrationservice.models.repositories.RegistrationDetailRepository;
import org.example.registrationservice.models.repositories.RegistrationRepository;
import org.example.registrationservice.models.services.RegistrationService;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final RegistrationDetailRepository registrationDetailRepository;
    private final EventGatewayService eventGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${registration.kafka.registration-created-topic:registration-created}")
    private String registrationCreatedTopic = "registration-created";

    public void setRegistrationCreatedTopic(String registrationCreatedTopic) {
        this.registrationCreatedTopic = registrationCreatedTopic;
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationResponse createRegistration(CreateRegistrationRequest request) {
        List<ResolvedItem> resolvedItems = request.items().stream()
                .map(this::resolveEvent)
                .toList();

        double total = resolvedItems.stream()
                .mapToDouble(item -> item.event().ticketPrice() * item.request().quantity())
                .sum();

        Registration registration = registrationRepository.save(Registration.builder()
                .participantName(request.participantName().trim())
                .participantEmail(request.participantEmail().trim())
                .totalAmount(total)
                .status(RegistrationStatus.PENDING)
                .build());

        List<RegistrationDetail> details = resolvedItems.stream()
                .map(item -> RegistrationDetail.builder()
                        .registration(registration)
                        .eventId(item.event().id())
                        .quantity(item.request().quantity())
                        .ticketPrice(item.event().ticketPrice())
                        .lineTotal(item.event().ticketPrice() * item.request().quantity())
                        .build())
                .toList();

        List<RegistrationDetail> savedDetails = registrationDetailRepository.saveAll(details);
        List<RegistrationDetailResponse> itemResponses = getRegistrationDetailResponses(savedDetails, resolvedItems);

        kafkaTemplate.send(registrationCreatedTopic, registration.getParticipantName());
        log.info("Sent rental-created event to topic '{}' with payload '{}'", registrationCreatedTopic, registration.getParticipantEmail());

        return new RegistrationResponse(
                registration.getId(),
                registration.getParticipantName(),
                registration.getParticipantEmail(),
                registration.getTotalAmount(),
                registration.getStatus(),
                itemResponses);
    }

    private static @NonNull List<RegistrationDetailResponse> getRegistrationDetailResponses(List<RegistrationDetail> savedDetails,
                                                                                List<ResolvedItem> resolvedItems) {
        List<RegistrationDetailResponse> itemResponses = new ArrayList<>(savedDetails.size());

        for (int index = 0; index < savedDetails.size(); index++) {
            RegistrationDetail detail = savedDetails.get(index);
            EventResponse eventResponse = resolvedItems.get(index).event();
            itemResponses.add(new RegistrationDetailResponse(
                    detail.getId(),
                    detail.getEventId(),
                    detail.getQuantity(),
                    eventResponse.ticketPrice(),
                    eventResponse.ticketPrice() * detail.getQuantity()
                    ));
        }
        return itemResponses;
    }

    private ResolvedItem resolveEvent(CreateRegistrationDetailRequest request) {
        EventResponse event = eventGatewayService.getEventById(request.eventId());
        return new ResolvedItem(request, event);
    }

    private record ResolvedItem(CreateRegistrationDetailRequest request, EventResponse event) {
    }
}

