package com.company.virtual.card.service;

import com.company.virtual.card.service.dto.CardResponse;
import com.company.virtual.card.service.dto.CreateCardRequest;
import com.company.virtual.card.service.dto.SpendResponse;
import com.company.virtual.card.service.services.CardService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for concurrent card operations
 * 
 * PURPOSE: Verify that the optimistic locking mechanism (@Version) prevents race conditions
 * and ensures the "no overdraft" rule is NEVER violated even under concurrent access.
 * 
 * APPROACH:
 * 1. Create a card with initial balance
 * 2. Simulate multiple threads trying to spend simultaneously
 * 3. Verify that the total spent never exceeds available balance
 * 4. Confirm some transactions fail due to optimistic locking or insufficient funds
 */
@Slf4j
@SpringBootTest
class CardConcurrencyTest {

    @Autowired
    private CardService cardService;

    @Test
    @DisplayName("Concurrency Test: Should prevent overdraft when multiple threads spend simultaneously")
    void testConcurrentSpends_PreventOverdraft() throws InterruptedException {
        // ========== SETUP ==========
        // Create a card with 100.00 balance
        CreateCardRequest createRequest = new CreateCardRequest();
        createRequest.setCardholderName("Concurrent Test User");
        createRequest.setInitialBalance(new BigDecimal("100.00"));
        CardResponse card = cardService.createCard(createRequest);
        
        log.info("Created card {} with balance 100.00", card.getId());

        // ========== TEST SCENARIO ==========
        // Launch 5 threads, each trying to spend 30.00
        // Expected: Total requests = 150.00, Available = 100.00
        // Result: Only 3 transactions should succeed (3 × 30 = 90), 2 must fail
        
        int numberOfThreads = 5;
        BigDecimal spendAmount = new BigDecimal("30.00");
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1); // Ensures all threads start simultaneously
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // ========== EXECUTE CONCURRENT SPENDS ==========
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNumber = i + 1;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for signal to start (ensures true concurrency)
                    
                    log.info("Thread {} attempting to spend {}", threadNumber, spendAmount);
                    SpendResponse response = cardService.spend(card.getId(), spendAmount);
                    
                    successCount.incrementAndGet();
                    log.info("Thread {} SUCCESS - Remaining balance: {}", threadNumber, response.getRemainingBalance());
                    
                } catch (IllegalArgumentException e) {
                    // Insufficient funds
                    failureCount.incrementAndGet();
                    log.warn("Thread {} FAILED - Reason: {}", threadNumber, e.getMessage());
                    
                } catch (Exception e) {
                    // Optimistic locking failure (ObjectOptimisticLockingFailureException)
                    failureCount.incrementAndGet();
                    log.warn("Thread {} FAILED - Concurrency conflict: {}", threadNumber, e.getClass().getSimpleName());
                    
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete (max 10 seconds)
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // ========== VERIFICATION ==========
        assertTrue(completed, "All threads should complete within timeout");
        
        int totalSuccessful = successCount.get();
        int totalFailed = failureCount.get();
        
        log.info("========== CONCURRENCY TEST RESULTS ==========");
        log.info("Successful transactions: {}", totalSuccessful);
        log.info("Failed transactions: {}", totalFailed);
        log.info("Total attempts: {}", totalSuccessful + totalFailed);
        
        // Get final card state
        CardResponse finalCard = cardService.getCard(card.getId());
        log.info("Final balance: {}", finalCard.getBalance());
        log.info("==============================================");

        // ========== ASSERTIONS ==========
        
        // 1. All attempts should be accounted for
        assertEquals(numberOfThreads, totalSuccessful + totalFailed, 
                "Total transactions should equal number of threads");
        
        // 2. At least one transaction should fail (since 5 × 30 = 150 > 100)
        assertTrue(totalFailed > 0, 
                "At least one transaction should fail due to insufficient funds or concurrency");
        
        // 3. CRITICAL: Final balance should NEVER be negative (no overdraft!)
        assertTrue(finalCard.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Balance must never go negative - NO OVERDRAFT ALLOWED");
        
        // 4. Balance calculation check: Initial - (Successful × Amount) = Final
        BigDecimal expectedBalance = new BigDecimal("100.00")
                .subtract(spendAmount.multiply(new BigDecimal(totalSuccessful)));
        assertEquals(0, expectedBalance.compareTo(finalCard.getBalance()), 
                "Final balance should match calculation: 100 - (" + totalSuccessful + " × 30)");
        
        // 5. Maximum 3 transactions can succeed (3 × 30 = 90 ≤ 100)
        assertTrue(totalSuccessful <= 3, 
                "Maximum 3 transactions of 30.00 can fit in 100.00 balance");
    }

    @Test
    @DisplayName("Concurrency Test: Multiple small spends should be handled correctly")
    void testConcurrentSpends_SmallAmounts() throws InterruptedException {
        // ========== SETUP ==========
        CreateCardRequest createRequest = new CreateCardRequest();
        createRequest.setCardholderName("Small Spends Test");
        createRequest.setInitialBalance(new BigDecimal("50.00"));
        CardResponse card = cardService.createCard(createRequest);

        // ========== TEST SCENARIO ==========
        // Launch 10 threads, each spending 10.00
        // Due to optimistic locking, some will fail
        // Key requirement: Balance must never go negative
        
        int numberOfThreads = 10;
        BigDecimal spendAmount = new BigDecimal("10.00");
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // ========== EXECUTE ==========
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cardService.spend(card.getId(), spendAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected failures due to optimistic locking or insufficient funds
                    failureCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // ========== VERIFICATION ==========
        CardResponse finalCard = cardService.getCard(card.getId());
        
        log.info("Small spends test: {} successful, {} failed out of {} attempts", 
                successCount.get(), failureCount.get(), numberOfThreads);
        log.info("Final balance: {}", finalCard.getBalance());

        // CRITICAL: Balance must never go negative (no overdraft)
        assertTrue(finalCard.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Balance must never be negative - NO OVERDRAFT ALLOWED");
        
        // All attempts should be accounted for
        assertEquals(numberOfThreads, successCount.get() + failureCount.get(), 
                "All transaction attempts should be accounted for");
        
        // At least some should fail (since 10 × 10 = 100 > 50)
        assertTrue(failureCount.get() > 0, 
                "At least some transactions should fail due to insufficient funds or concurrency");
        
        // Balance calculation: Initial - (Successful × Amount) must equal Final
        BigDecimal expectedBalance = new BigDecimal("50.00")
                .subtract(spendAmount.multiply(new BigDecimal(successCount.get())));
        assertEquals(0, expectedBalance.compareTo(finalCard.getBalance()), 
                "Balance calculation must be accurate");
    }

    @Test
    @DisplayName("Concurrency Test: Simultaneous top-ups and spends should maintain consistency")
    void testConcurrentTopupsAndSpends() throws InterruptedException {
        // ========== SETUP ==========
        CreateCardRequest createRequest = new CreateCardRequest();
        createRequest.setCardholderName("Mixed Operations Test");
        createRequest.setInitialBalance(new BigDecimal("100.00"));
        CardResponse card = cardService.createCard(createRequest);

        // ========== TEST SCENARIO ==========
        // 3 threads spending 20.00 each
        // 3 threads topping up 20.00 each
        // This tests that concurrent reads/writes maintain data integrity
        
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(6);
        
        AtomicInteger spendSuccess = new AtomicInteger(0);
        AtomicInteger spendFailure = new AtomicInteger(0);
        AtomicInteger topupSuccess = new AtomicInteger(0);

        // Launch spend threads
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cardService.spend(card.getId(), new BigDecimal("20.00"));
                    spendSuccess.incrementAndGet();
                } catch (Exception e) {
                    // May fail due to optimistic locking
                    spendFailure.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Launch topup threads
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    cardService.topup(card.getId(), new BigDecimal("20.00"));
                    topupSuccess.incrementAndGet();
                } catch (Exception e) {
                    // Topups might fail due to optimistic locking (version conflict)
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // ========== VERIFICATION ==========
        CardResponse finalCard = cardService.getCard(card.getId());
        
        log.info("Mixed operations: {} spends succeeded, {} spends failed, {} topups succeeded", 
                spendSuccess.get(), spendFailure.get(), topupSuccess.get());
        log.info("Final balance: {}", finalCard.getBalance());

        // CRITICAL: Final balance must not be negative
        assertTrue(finalCard.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Balance must never be negative - NO OVERDRAFT ALLOWED");
        
        // Calculate expected balance: Initial + (Successful Topups × 20) - (Successful Spends × 20)
        BigDecimal expectedBalance = new BigDecimal("100.00")
                .add(new BigDecimal("20.00").multiply(new BigDecimal(topupSuccess.get())))
                .subtract(new BigDecimal("20.00").multiply(new BigDecimal(spendSuccess.get())));
        
        assertEquals(0, expectedBalance.compareTo(finalCard.getBalance()), 
                "Balance calculation must be accurate: 100 + (" + topupSuccess.get() + 
                " × 20) - (" + spendSuccess.get() + " × 20) = " + finalCard.getBalance());
    }
}
