package com.company.virtual.card.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("balance")
    private BigDecimal balance;
}
