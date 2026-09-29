package org.example.eventservice.models.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "events")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private Double ticketPrice;

    @Column(nullable = false)
    private Integer availableTickets;
}