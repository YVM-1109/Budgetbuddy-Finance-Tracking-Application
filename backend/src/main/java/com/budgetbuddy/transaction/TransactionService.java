package com.budgetbuddy.transaction;

import com.budgetbuddy.auth.User;
import com.budgetbuddy.common.error.ApiException;
import com.budgetbuddy.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Transaction use cases. Ownership is enforced on every read and write;
 * a transaction belonging to another user is indistinguishable from a
 * missing one (TECHNICAL_SPEC 7).
 */
@Service
public class TransactionService {

    private static final Set<String> SORTABLE = Set.of("transactionDate", "amount", "category", "createdAt");

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository,
                              CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public Page<TransactionResponse> list(int page, int size, TransactionType type, String category,
                                          LocalDate startDate, LocalDate endDate, String search,
                                          String sort, String direction) {
        UUID userId = currentUserService.currentUserId();
        String sortField = SORTABLE.contains(sort) ? sort : "transactionDate";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(dir, sortField));
        String trimmedSearch = (search == null || search.isBlank()) ? null : search.trim();
        String trimmedCategory = (category == null || category.isBlank()) ? null : category.trim();
        return transactionRepository
                .search(userId, type, trimmedCategory, startDate, endDate, trimmedSearch, pageable)
                .map(TransactionResponse::from);
    }

    public TransactionResponse get(UUID id) {
        UUID userId = currentUserService.currentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found."));
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        UUID userId = currentUserService.currentUserId();
        User user = currentUserService.currentUser();
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        applyRequest(transaction, request);
        transaction.setSourceType(TransactionSource.MANUAL);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        UUID userId = currentUserService.currentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found."));
        applyRequest(transaction, request);
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = currentUserService.currentUserId();
        if (transactionRepository.findByIdAndUserId(id, userId).isEmpty()) {
            throw ApiException.notFound("Transaction not found.");
        }
        transactionRepository.deleteByIdAndUserId(id, userId);
    }

    private void applyRequest(Transaction transaction, TransactionRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero.");
        }
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setCategory(request.category().trim());
        transaction.setRemarks(request.remarks());
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
