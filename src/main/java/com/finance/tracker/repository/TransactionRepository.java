package com.finance.tracker.repository;

import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

        Page<Transaction> findByUserId(Long userId, Pageable pageable);

        Page<Transaction> findByUserIdAndType(Long userId, TransactionType type, Pageable pageable);

        Page<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId, Pageable pageable);

        Page<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDateTime startDate,
                        LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
                        "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
                        "AND (:type IS NULL OR t.type = :type) " +
                        "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
                        "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
                        "AND (:keyword IS NULL OR :keyword = '' OR " +
                        "LOWER(t.note) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(t.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Transaction> findWithFilters(@Param("userId") Long userId,
                        @Param("categoryId") Long categoryId,
                        @Param("type") TransactionType type,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId " +
                        "AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
        BigDecimal sumAmountByUserIdAndTypeAndDateBetween(@Param("userId") Long userId,
                        @Param("type") TransactionType type,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT SUM(CASE WHEN t.type = com.finance.tracker.entity.TransactionType.INCOME THEN t.amount ELSE -t.amount END) "
                        +
                        "FROM Transaction t WHERE t.user.id = :userId " +
                        "AND (t.transactionDate < :date OR (t.transactionDate = :date AND (:id IS NULL OR t.id <= :id)))")
        BigDecimal getCumulativeBalanceAt(@Param("userId") Long userId, @Param("date") LocalDateTime date,
                        @Param("id") Long id);

        List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDateTime startDate,
                        LocalDateTime endDate);
}
