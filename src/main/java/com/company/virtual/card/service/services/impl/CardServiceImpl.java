package com.company.virtual.card.service.services.impl;

import com.company.virtual.card.service.dto.CardResponse;
import com.company.virtual.card.service.dto.CreateCardRequest;
import com.company.virtual.card.service.dto.SpendResponse;
import com.company.virtual.card.service.dto.TopupResponse;
import com.company.virtual.card.service.dto.TransactionResponse;
import com.company.virtual.card.service.entity.Card;
import com.company.virtual.card.service.entity.CardTransaction;
import com.company.virtual.card.service.enums.TransactionType;
import com.company.virtual.card.service.repository.CardRepository;
import com.company.virtual.card.service.repository.TransactionRepository;
import com.company.virtual.card.service.services.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        log.info("Creating new card for user: {}", request.getCardholderName());

        Card card = Card.builder()
                .cardholderName(request.getCardholderName())
                .balance(request.getInitialBalance())
                .build();
        
        Card savedCard = cardRepository.save(card);
        log.debug("Card saved with ID: {}", savedCard.getId());

        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(savedCard, request.getInitialBalance(), TransactionType.CREDIT);
            log.info("Initial balance credited for Card ID: {}", savedCard.getId());
        }

        return convertToCardResponse(savedCard);
    }

    public CardResponse getCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Card lookup failed for ID: {}", id);
                    return new NoSuchElementException("Card not found with ID: " + id);
                });
        return convertToCardResponse(card);
    }

    @Transactional
    public SpendResponse spend(Long cardId, BigDecimal amount) {
        log.info("Processing spend request. CardID: {}, Amount: {}", cardId, amount);
        
        Card card = findCardById(cardId);

        //Strict Overdraft Check
        if (card.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds. CardID: {}, Balance: {}, Request: {}", 
                     cardId, card.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient funds. Available: " + card.getBalance());
        }

        BigDecimal newBalance = card.getBalance().subtract(amount);
        card.setBalance(newBalance);
        
        Card savedCard = cardRepository.save(card); // Triggers Optimistic Lock
        recordTransaction(savedCard, amount, TransactionType.DEBIT);
        
        log.info("Spend successful. New Balance: {}", newBalance);
        return SpendResponse.builder()
                .id(savedCard.getId())
                .remainingBalance(savedCard.getBalance())
                .build();
    }

    @Transactional
    public TopupResponse topup(Long cardId, BigDecimal amount) {
        log.info("Processing top-up. CardID: {}, Amount: {}", cardId, amount);

        Card card = findCardById(cardId);
        
        BigDecimal newBalance = card.getBalance().add(amount);
        card.setBalance(newBalance);
        
        Card savedCard = cardRepository.save(card);
        recordTransaction(savedCard, amount, TransactionType.CREDIT);
        
        log.info("Top-up successful. New Balance: {}", newBalance);
        return TopupResponse.builder()
                .id(savedCard.getId())
                .balance(savedCard.getBalance())
                .build();
    }

    public List<TransactionResponse> getTransactions(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new NoSuchElementException("Card not found with ID: " + cardId);
        }
        
        List<CardTransaction> transactions = transactionRepository.findByCardIdOrderByTimestampDesc(cardId);

        return transactions.stream()
                .map(txn -> TransactionResponse.builder()
                        .id(txn.getId())
                        .amount(txn.getAmount())
                        .type(txn.getType())
                        .timestamp(txn.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    // Helper method to avoid code duplication
    private void recordTransaction(Card card, BigDecimal amount, TransactionType type) {
        CardTransaction transaction = CardTransaction.builder()
                .card(card)
                .amount(amount)
                .type(type)
                .build();
        transactionRepository.save(transaction);
    }

    // Helper method to find card by ID (returns entity for internal use)
    private Card findCardById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Card lookup failed for ID: {}", id);
                    return new NoSuchElementException("Card not found with ID: " + id);
                });
    }

    // Helper method to convert Card entity to CardResponse DTO
    private CardResponse convertToCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .cardholderName(card.getCardholderName())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .build();
    }
}