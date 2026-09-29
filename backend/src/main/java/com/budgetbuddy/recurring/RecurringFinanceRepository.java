package com.budgetbuddy.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringFinanceRepository extends JpaRepository<RecurringFinance, UUID> {

    List<RecurringFinance> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<RecurringFinance> findByIdAndUserId(UUID id, UUID userId);
}
