package com.company.virtual.card.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("cardholderName")
    private String cardholderName;

    @JsonProperty("balance")
    private BigDecimal balance;

    @JsonProperty("createdAt")
    private Instant createdAt;
}
