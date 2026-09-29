package com.budgetbuddy.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * All queries are scoped by userId - data isolation is enforced here,
 * not in the frontend (TECHNICAL_SPEC 7).
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND (:type IS NULL OR t.type = :type)
              AND (:category IS NULL OR t.category = :category)
              AND (:startDate IS NULL OR t.transactionDate >= :startDate)
              AND (:endDate IS NULL OR t.transactionDate <= :endDate)
              AND (:search IS NULL OR LOWER(COALESCE(t.remarks, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(t.category) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Transaction> search(@Param("userId") UUID userId,
                             @Param("type") TransactionType type,
                             @Param("category") String category,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("search") String search,
                             Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.user.id = :userId AND t.type = :type
              AND t.transactionDate >= :start AND t.transactionDate <= :end
            """)
    BigDecimal sumByTypeBetween(@Param("userId") UUID userId,
                                @Param("type") TransactionType type,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end);

    interface MonthlyTotal {
        Integer getYear();
        Integer getMonth();
        BigDecimal getTotal();
    }

    @Query("""
            SELECT year(t.transactionDate) AS year,
                   month(t.transactionDate) AS month,
                   SUM(t.amount) AS total
            FROM Transaction t
            WHERE t.user.id = :userId AND t.type = :type
              AND t.transactionDate >= :start AND t.transactionDate <= :end
            GROUP BY year(t.transactionDate), month(t.transactionDate)
            ORDER BY year(t.transactionDate), month(t.transactionDate)
            """)
    List<MonthlyTotal> sumMonthlyByType(@Param("userId") UUID userId,
                                        @Param("type") TransactionType type,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    interface CategoryTotal {
        String getCategory();
        BigDecimal getTotal();
    }

    @Query("""
            SELECT t.category AS category, SUM(t.amount) AS total
            FROM Transaction t
            WHERE t.user.id = :userId AND t.type = :type
              AND t.transactionDate >= :start AND t.transactionDate <= :end
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<CategoryTotal> sumByCategory(@Param("userId") UUID userId,
                                      @Param("type") TransactionType type,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    List<Transaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, Pageable pageable);
}
