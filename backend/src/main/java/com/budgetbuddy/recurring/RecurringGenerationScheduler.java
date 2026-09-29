package com.budgetbuddy.recurring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler entry point (TECHNICAL_SPEC 10). Runs after the day rolls over
 * in the reporting timezone; generation itself is idempotent, so overlapping
 * or repeated executions are safe. On Render free tier the process may be
 * down when a day rolls over - catch-up happens on the next startup/pass.
 */
@Component
public class RecurringGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringGenerationScheduler.class);

    private final RecurringGenerationService generationService;

    public RecurringGenerationScheduler(RecurringGenerationService generationService) {
        this.generationService = generationService;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Kolkata")
    public void runDaily() {
        log.info("Scheduled recurring generation starting");
        generationService.generateForAllUsers();
    }
}
