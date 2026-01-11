package com.company.virtual.card.service.repository;

import com.company.virtual.card.service.entity.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<CardTransaction, Long> {
    List<CardTransaction> findByCardIdOrderByTimestampDesc(Long cardId);
}