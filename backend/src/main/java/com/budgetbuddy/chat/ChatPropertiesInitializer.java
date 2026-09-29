package com.budgetbuddy.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Startup configuration validation (TECHNICAL_SPEC 16): warns when chat
 * cannot work because no OpenAI key is configured.
 */
@Component
public class ChatPropertiesInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ChatPropertiesInitializer.class);

    private final ChatProperties chatProperties;

    public ChatPropertiesInitializer(ChatProperties chatProperties) {
        this.chatProperties = chatProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (chatProperties.openaiApiKey() == null || chatProperties.openaiApiKey().isBlank()) {
            log.warn("OPENAI_API_KEY is not configured - /api/chat will return 503 until it is set.");
        }
    }
}
