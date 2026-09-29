package com.budgetbuddy.chat;

import com.budgetbuddy.common.error.ApiException;
import com.budgetbuddy.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI integration via the Responses API (current recommended interface -
 * TECHNICAL_SPEC 12). The API key stays on the backend; no user financial
 * data is attached to requests (PRD 13 / DEC-016).
 */
@Component
public class OpenAiChatClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatClient.class);

    private static final URI RESPONSES_URL = URI.create("https://api.openai.com/v1/responses");
    private static final String SYSTEM_PROMPT =
            "You are BudgetBuddy's general financial assistant. Provide clear, practical, general "
            + "financial guidance. You do not have access to the user's private financial data; never "
            + "claim otherwise. Include a brief note that this is general information, not professional "
            + "financial advice, when giving substantive guidance.";

    private final RestClient restClient;
    private final ChatProperties properties;

    public OpenAiChatClient(ChatProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .requestFactory(new JdkClientHttpRequestFactory(
                        java.net.http.HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build()))
                .build();
    }

    public String complete(String userMessage) {
        if (properties.openaiApiKey() == null || properties.openaiApiKey().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Chat is not configured on the server.");
        }
        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "instructions", SYSTEM_PROMPT,
                "input", userMessage,
                "store", false
        );
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(RESPONSES_URL)
                    .header("Authorization", "Bearer " + properties.openaiApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            return extractText(response);
        } catch (RestClientResponseException e) {
            int upstream = e.getStatusCode().value();
            if (upstream == 429) {
                // Provider quota exhausted or rate limited (API_SPEC: 429).
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED,
                        "The chat provider's quota is exhausted or rate-limited. Please try again later.");
            }
            if (upstream == 401 || upstream == 403) {
                log.error("OpenAI rejected the configured API key (HTTP {}).", upstream);
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CHAT_UPSTREAM_ERROR,
                        "Chat is misconfigured on the server.");
            }
            log.warn("OpenAI request failed with HTTP {}", upstream);
            throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Chat provider is unavailable. Please retry shortly.");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Chat provider is unavailable. Please retry shortly.");
        }
    }

    /**
     * Reads the assistant text from the typed Responses API output array:
     * the first message item's output_text content parts.
     */
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        Object outputObj = response != null ? response.get("output") : null;
        if (outputObj instanceof List<?> output) {
            StringBuilder text = new StringBuilder();
            for (Object itemObj : output) {
                if (itemObj instanceof Map<?, ?> item && "message".equals(item.get("type"))) {
                    Object contentObj = item.get("content");
                    if (contentObj instanceof List<?> content) {
                        for (Object partObj : content) {
                            if (partObj instanceof Map<?, ?> part && "output_text".equals(part.get("type"))) {
                                Object t = part.get("text");
                                if (t != null) {
                                    text.append(t);
                                }
                            }
                        }
                    }
                }
            }
            if (text.length() > 0) {
                return text.toString();
            }
        }
        throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.CHAT_UPSTREAM_ERROR,
                "Chat provider returned an unexpected response.");
    }
}
