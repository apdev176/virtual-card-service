package com.company.virtual.card.service.controller;

import com.company.virtual.card.service.dto.CardResponse;
import com.company.virtual.card.service.dto.CreateCardRequest;
import com.company.virtual.card.service.dto.SpendRequest;
import com.company.virtual.card.service.dto.SpendResponse;
import com.company.virtual.card.service.dto.TopupResponse;
import com.company.virtual.card.service.dto.TransactionResponse;
import com.company.virtual.card.service.services.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        CardResponse cardResponse = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardResponse);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardResponse> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }

    @PostMapping(value = "/{id}/spend", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpendResponse> spend(@PathVariable Long id, 
                                                @Valid @RequestBody SpendRequest request) {
        SpendResponse spendResponse = cardService.spend(id, request.getAmount());
        return ResponseEntity.ok(spendResponse);
    }

    @PostMapping(value = "/{id}/topup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TopupResponse> topup(@PathVariable Long id, 
                                                @Valid @RequestBody SpendRequest request) {
        TopupResponse topupResponse = cardService.topup(id, request.getAmount());
        return ResponseEntity.ok(topupResponse);
    }

    @GetMapping(value = "/{id}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getTransactions(id));
    }
}