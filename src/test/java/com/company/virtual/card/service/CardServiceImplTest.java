package com.company.virtual.card.service;

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
import com.company.virtual.card.service.services.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CardServiceImpl
 * Tests cover all business logic requirements including the critical "no overdraft" rule
 */
@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card testCard;
    private CreateCardRequest createCardRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        testCard = Card.builder()
                .id(1L)
                .cardholderName("Alice")
                .balance(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .version(1L)
                .build();

        createCardRequest = new CreateCardRequest();
        createCardRequest.setCardholderName("Alice");
        createCardRequest.setInitialBalance(new BigDecimal("100.00"));
    }

    // ==================== CREATE CARD TESTS ====================

    @Test
    @DisplayName("Should create card with valid initial balance")
    void testCreateCard_Success() {
        // Arrange
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(transactionRepository.save(any(CardTransaction.class))).thenReturn(new CardTransaction());

        // Act
        CardResponse response = cardService.createCard(createCardRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Alice", response.getCardholderName());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getBalance()));
        assertNotNull(response.getCreatedAt());

        // Verify interactions
        verify(cardRepository, times(1)).save(any(Card.class));
        verify(transactionRepository, times(1)).save(any(CardTransaction.class)); // Initial CREDIT transaction
    }

    @Test
    @DisplayName("Should create card with zero initial balance without transaction")
    void testCreateCard_ZeroBalance() {
        // Arrange
        createCardRequest.setInitialBalance(BigDecimal.ZERO);
        Card zeroBalanceCard = Card.builder()
                .id(1L)
                .cardholderName("Bob")
                .balance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();
        when(cardRepository.save(any(Card.class))).thenReturn(zeroBalanceCard);

        // Act
        CardResponse response = cardService.createCard(createCardRequest);

        // Assert
        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getBalance()));

        // Verify no transaction is created for zero balance
        verify(transactionRepository, never()).save(any(CardTransaction.class));
    }

    // ==================== GET CARD TESTS ====================

    @Test
    @DisplayName("Should retrieve card by valid ID")
    void testGetCard_Success() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // Act
        CardResponse response = cardService.getCard(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Alice", response.getCardholderName());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getBalance()));
    }

    @Test
    @DisplayName("Should throw exception when card not found")
    void testGetCard_NotFound() {
        // Arrange
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> {
            cardService.getCard(999L);
        });

        assertTrue(exception.getMessage().contains("Card not found"));
    }

    // ==================== SPEND TESTS (CRITICAL - NO OVERDRAFT RULE) ====================

    @Test
    @DisplayName("Should successfully spend when sufficient balance")
    void testSpend_SufficientBalance() {
        // Arrange
        BigDecimal spendAmount = new BigDecimal("30.00");
        BigDecimal expectedBalance = new BigDecimal("70.00");
        
        Card updatedCard = Card.builder()
                .id(1L)
                .cardholderName("Alice")
                .balance(expectedBalance)
                .createdAt(Instant.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(updatedCard);
        when(transactionRepository.save(any(CardTransaction.class))).thenReturn(new CardTransaction());

        // Act
        SpendResponse response = cardService.spend(1L, spendAmount);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(0, expectedBalance.compareTo(response.getRemainingBalance()));

        // Verify DEBIT transaction was recorded
        verify(transactionRepository, times(1)).save(any(CardTransaction.class));
    }

    @Test
    @DisplayName("Should allow spending exact balance (edge case)")
    void testSpend_ExactBalance() {
        // Arrange
        BigDecimal spendAmount = new BigDecimal("100.00"); // Exact balance
        Card zeroBalanceCard = Card.builder()
                .id(1L)
                .cardholderName("Alice")
                .balance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(zeroBalanceCard);
        when(transactionRepository.save(any(CardTransaction.class))).thenReturn(new CardTransaction());

        // Act
        SpendResponse response = cardService.spend(1L, spendAmount);

        // Assert
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getRemainingBalance()));
    }

    @Test
    @DisplayName("Should reject spend when insufficient balance (CRITICAL TEST)")
    void testSpend_InsufficientBalance() {
        // Arrange
        BigDecimal spendAmount = new BigDecimal("150.00"); // More than balance
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cardService.spend(1L, spendAmount);
        });

        assertTrue(exception.getMessage().contains("Insufficient funds"));
        
        // Verify no transaction was recorded (rollback scenario)
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(CardTransaction.class));
    }

    @Test
    @DisplayName("Should reject spend when amount exceeds balance by small margin (precision test)")
    void testSpend_InsufficientBalance_SmallMargin() {
        // Arrange
        BigDecimal spendAmount = new BigDecimal("100.01"); // Just 1 cent over
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cardService.spend(1L, spendAmount);
        });

        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Should throw exception when spending from non-existent card")
    void testSpend_CardNotFound() {
        // Arrange
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            cardService.spend(999L, new BigDecimal("10.00"));
        });
    }

    // ==================== TOP-UP TESTS ====================

    @Test
    @DisplayName("Should successfully top-up card")
    void testTopup_Success() {
        // Arrange
        BigDecimal topupAmount = new BigDecimal("50.00");
        BigDecimal expectedBalance = new BigDecimal("150.00");
        
        Card toppedUpCard = Card.builder()
                .id(1L)
                .cardholderName("Alice")
                .balance(expectedBalance)
                .createdAt(Instant.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(toppedUpCard);
        when(transactionRepository.save(any(CardTransaction.class))).thenReturn(new CardTransaction());

        // Act
        TopupResponse response = cardService.topup(1L, topupAmount);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(0, expectedBalance.compareTo(response.getBalance()));

        // Verify CREDIT transaction was recorded
        verify(transactionRepository, times(1)).save(any(CardTransaction.class));
    }

    @Test
    @DisplayName("Should throw exception when topping up non-existent card")
    void testTopup_CardNotFound() {
        // Arrange
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            cardService.topup(999L, new BigDecimal("50.00"));
        });
    }

    // ==================== TRANSACTION HISTORY TESTS ====================

    @Test
    @DisplayName("Should retrieve transaction history for card")
    void testGetTransactions_Success() {
        // Arrange
        CardTransaction tx1 = CardTransaction.builder()
                .id(1L)
                .card(testCard)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.CREDIT)
                .timestamp(Instant.now())
                .build();

        CardTransaction tx2 = CardTransaction.builder()
                .id(2L)
                .card(testCard)
                .amount(new BigDecimal("30.00"))
                .type(TransactionType.DEBIT)
                .timestamp(Instant.now())
                .build();

        List<CardTransaction> transactions = Arrays.asList(tx2, tx1); // Newest first

        when(cardRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByCardIdOrderByTimestampDesc(1L)).thenReturn(transactions);

        // Act
        List<TransactionResponse> response = cardService.getTransactions(1L);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(TransactionType.DEBIT, response.get(0).getType()); // Newest first
        assertEquals(TransactionType.CREDIT, response.get(1).getType());
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void testGetTransactions_EmptyList() {
        // Arrange
        when(cardRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByCardIdOrderByTimestampDesc(1L)).thenReturn(Arrays.asList());

        // Act
        List<TransactionResponse> response = cardService.getTransactions(1L);

        // Assert
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when getting transactions for non-existent card")
    void testGetTransactions_CardNotFound() {
        // Arrange
        when(cardRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            cardService.getTransactions(999L);
        });
    }

    // ==================== EDGE CASE & BOUNDARY TESTS ====================

    @Test
    @DisplayName("Should handle very small amounts (0.01) correctly")
    void testSpend_SmallAmount() {
        // Arrange
        BigDecimal spendAmount = new BigDecimal("0.01");
        BigDecimal expectedBalance = new BigDecimal("99.99");
        
        Card updatedCard = Card.builder()
                .id(1L)
                .balance(expectedBalance)
                .createdAt(Instant.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(updatedCard);
        when(transactionRepository.save(any(CardTransaction.class))).thenReturn(new CardTransaction());

        // Act
        SpendResponse response = cardService.spend(1L, spendAmount);

        // Assert
        assertEquals(0, expectedBalance.compareTo(response.getRemainingBalance()));
    }

    @Test
    @DisplayName("Should handle large amounts (precision test)")
    void testTopup_LargeAmount() {
        // Arrange
        BigDecimal topupAmount = new BigDecimal("9999999.99");
        BigDecimal expectedBalance = new BigDecimal("10000099.99");
        
        Card updatedCard = Card.builder()
                .id(1L)
                .balance(expectedBalance)
                .createdAt(Instant.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(updatedCard);
        when(transactionRepository.save(any(CardTransaction.class))).thenReturn(new CardTransaction());

        // Act
        TopupResponse response = cardService.topup(1L, topupAmount);

        // Assert
        assertEquals(0, expectedBalance.compareTo(response.getBalance()));
    }
}
