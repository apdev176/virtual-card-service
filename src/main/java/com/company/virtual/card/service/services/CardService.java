package com.company.virtual.card.service.services;

import com.company.virtual.card.service.dto.CreateCardRequest;
import com.company.virtual.card.service.dto.TransactionResponse;
import com.company.virtual.card.service.entity.Card;

import java.math.BigDecimal;
import java.util.List;

public interface CardService {
    Card createCard(CreateCardRequest request);
    Card getCard(Long id);
    Card spend(Long cardId, BigDecimal amount);
    Card topup(Long cardId, BigDecimal amount);
    List<TransactionResponse> getTransactions(Long cardId);
}
