package com.company.virtual.card.service.services;

import com.company.virtual.card.service.dto.CardResponse;
import com.company.virtual.card.service.dto.CreateCardRequest;
import com.company.virtual.card.service.dto.SpendResponse;
import com.company.virtual.card.service.dto.TopupResponse;
import com.company.virtual.card.service.dto.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CardService {
    CardResponse createCard(CreateCardRequest request);
    CardResponse getCard(Long id);
    SpendResponse spend(Long cardId, BigDecimal amount);
    TopupResponse topup(Long cardId, BigDecimal amount);
    List<TransactionResponse> getTransactions(Long cardId);
}
