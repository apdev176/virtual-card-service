package com.company.virtual.card.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cardholderName;

    // Precision 19, Scale 2 ensures strict financial format
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

}