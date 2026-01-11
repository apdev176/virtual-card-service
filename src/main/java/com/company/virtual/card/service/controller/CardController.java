package com.company.virtual.card.service.controller;

import com.company.virtual.card.service.dto.CreateCardRequest;
import com.company.virtual.card.service.dto.SpendRequest;
import com.company.virtual.card.service.dto.TransactionResponse;
import com.company.virtual.card.service.entity.Card;
import com.company.virtual.card.service.services.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<Card> createCard(@Valid @RequestBody CreateCardRequest request) {
        Card card = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }

    @PostMapping("/{id}/spend")
    public ResponseEntity<Map<String, Object>> spend(@PathVariable Long id, 
                                                     @Valid @RequestBody SpendRequest request) {
        Card updatedCard = cardService.spend(id, request.getAmount());
        return ResponseEntity.ok(Map.of(
            "id", updatedCard.getId(),
            "remainingBalance", updatedCard.getBalance()
        ));
    }

    @PostMapping("/{id}/topup")
    public ResponseEntity<Map<String, Object>> topup(@PathVariable Long id, 
                                                     @Valid @RequestBody SpendRequest request) {
        // Reusing SpendRequest as it just contains "amount"
        Card updatedCard = cardService.topup(id, request.getAmount());
        return ResponseEntity.ok(Map.of(
            "id", updatedCard.getId(),
            "balance", updatedCard.getBalance()
        ));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getTransactions(id));
    }
}