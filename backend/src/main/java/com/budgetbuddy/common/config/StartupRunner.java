package com.budgetbuddy.common.config;

import com.budgetbuddy.recurring.RecurringGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Reconciles missed recurring periods after downtime (ASM-003, PRD 8.4):
 * if the app was unavailable when periods came due, generation runs on
 * startup and is idempotent, so nothing is duplicated.
 */
@Component
public class StartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRunner.class);

    private final RecurringGenerationService generationService;

    public StartupRunner(RecurringGenerationService generationService) {
        this.generationService = generationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Startup recurring catch-up starting");
            generationService.generateForAllUsers();
        } catch (Exception e) {
            log.error("Startup recurring catch-up failed", e);
        }
    }
}
