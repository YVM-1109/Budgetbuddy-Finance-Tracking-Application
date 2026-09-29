package com.budgetbuddy.recurring;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Recurring finance endpoints (API_SPEC).
 */
@RestController
@RequestMapping("/api/recurring-finances")
public class RecurringFinanceController {

    private final RecurringFinanceService recurringFinanceService;

    public RecurringFinanceController(RecurringFinanceService recurringFinanceService) {
        this.recurringFinanceService = recurringFinanceService;
    }

    @GetMapping
    public List<RecurringFinanceResponse> list() {
        return recurringFinanceService.list();
    }

    @PostMapping
    public ResponseEntity<RecurringFinanceResponse> create(@Valid @RequestBody RecurringFinanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringFinanceService.create(request));
    }

    @PutMapping("/{id}")
    public RecurringFinanceResponse update(@PathVariable UUID id,
                                           @Valid @RequestBody RecurringFinanceRequest request) {
        return recurringFinanceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        recurringFinanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
