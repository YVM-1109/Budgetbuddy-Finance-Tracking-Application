package com.budgetbuddy.recurring;

import com.budgetbuddy.auth.User;
import com.budgetbuddy.common.error.ApiException;
import com.budgetbuddy.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Recurring finance use cases. Deactivation/deletion never removes
 * historical generated transactions (DATA_MODEL section 7).
 */
@Service
public class RecurringFinanceService {

    private static final Logger log = LoggerFactory.getLogger(RecurringFinanceService.class);

    private final RecurringFinanceRepository recurringRepository;
    private final RecurringGenerationService generationService;
    private final CurrentUserService currentUserService;

    public RecurringFinanceService(RecurringFinanceRepository recurringRepository,
                                   RecurringGenerationService generationService,
                                   CurrentUserService currentUserService) {
        this.recurringRepository = recurringRepository;
        this.generationService = generationService;
        this.currentUserService = currentUserService;
    }

    public List<RecurringFinanceResponse> list() {
        UUID userId = currentUserService.currentUserId();
        return recurringRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RecurringFinanceResponse::from)
                .toList();
    }

    @Transactional
    public RecurringFinanceResponse create(RecurringFinanceRequest request) {
        User user = currentUserService.currentUser();
        validate(request);
        RecurringFinance config = new RecurringFinance();
        applyRequest(config, request);
        config.setUser(user);
        config = recurringRepository.save(config);
        // Generate immediately for any periods already due (including the
        // current month once its start day has passed); scheduler catches up later.
        generationService.generateForConfig(config.getId());
        return RecurringFinanceResponse.from(recurringRepository.findById(config.getId()).orElse(config));
    }

    @Transactional
    public RecurringFinanceResponse update(UUID id, RecurringFinanceRequest request) {
        UUID userId = currentUserService.currentUserId();
        RecurringFinance config = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Recurring finance not found."));
        validate(request);
        BigDecimal oldAmount = config.getAmount();
        applyRequest(config, request);
        if (request.amount() != null && oldAmount.compareTo(request.amount()) != 0) {
            // DEC-009: amount changes affect future generation only; historical
            // generated transactions are never rewritten.
            log.info("Recurring finance {} amount changed from {} to {} - future periods only",
                    config.getId(), oldAmount, request.amount());
        }
        config = recurringRepository.save(config);
        // A reactivated or future-dated config may have new due periods.
        if (config.isActive()) {
            generationService.generateForConfig(config.getId());
        }
        return RecurringFinanceResponse.from(config);
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = currentUserService.currentUserId();
        RecurringFinance config = recurringRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Recurring finance not found."));
        // Stop future generation but preserve generated history: the unique
        // generation records keep their transactions intact (DATA_MODEL 7).
        recurringRepository.delete(config);
    }

    private void validate(RecurringFinanceRequest request) {
        if (request.amount() != null && request.amount().signum() <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero.");
        }
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("End date must not be before the start date.");
        }
    }

    private void applyRequest(RecurringFinance config, RecurringFinanceRequest request) {
        if (request.type() != null) config.setType(request.type());
        if (request.amount() != null) config.setAmount(request.amount());
        if (request.category() != null) config.setCategory(request.category().trim());
        if (request.startDate() != null) config.setStartDate(request.startDate());
        config.setEndDate(request.endDate());
        if (request.active() != null) config.setActive(request.active());
        else if (config.getCreatedAt() == null) config.setActive(true);
    }
}
