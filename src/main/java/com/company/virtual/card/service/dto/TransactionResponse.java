package com.company.virtual.card.service.dto;

import com.company.virtual.card.service.enums.TransactionType;
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
public class TransactionResponse {
    private Long id;
    private Long cardId;
    private BigDecimal amount;
    private TransactionType type;
    private Instant timestamp;
}
