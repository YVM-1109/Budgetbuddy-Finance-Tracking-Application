package com.budgetbuddy.chat;

import com.budgetbuddy.common.error.ApiException;
import com.budgetbuddy.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Chat use case: validates/limits input, calls OpenAI with a timeout, and
 * maps failures to the API_SPEC error contract. No user financial data is
 * ever attached (DEC-016).
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Per-user sliding-window rate limit. */
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int WINDOW_SECONDS = 60;

    private final OpenAiChatClient chatClient;
    private final ChatProperties properties;
    private final ConcurrentHashMap<UUID, Deque<Instant>> rateLimiter = new ConcurrentHashMap<>();

    public ChatService(OpenAiChatClient chatClient, ChatProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    public ChatResponse send(UUID userId, ChatRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isEmpty()) {
            throw ApiException.badRequest("Message must not be empty.");
        }
        int maxLength = properties.maxInputLength() > 0 ? properties.maxInputLength() : 2000;
        if (message.length() > maxLength) {
            throw ApiException.badRequest("Message is too long (max " + maxLength + " characters).");
        }
        enforceRateLimit(userId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> chatClient.complete(message));
        try {
            String reply = future.get(timeoutSeconds(), TimeUnit.SECONDS);
            return new ChatResponse(reply);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.CHAT_TIMEOUT,
                    "The chat service took too long to respond. Please retry.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Chat request was interrupted.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ApiException api) {
                throw api;
            }
            log.error("Chat upstream failure", e.getCause());
            throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.CHAT_UPSTREAM_ERROR,
                    "Chat provider failed. Please retry shortly.");
        } finally {
            executor.shutdownNow();
        }
    }

    private long timeoutSeconds() {
        return properties.timeoutSeconds() > 0 ? properties.timeoutSeconds() : 30;
    }

    private void enforceRateLimit(UUID userId) {
        Instant now = Instant.now();
        Deque<Instant> timestamps = rateLimiter.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED,
                        "Too many chat requests. Please wait a moment and try again.");
            }
            timestamps.addLast(now);
        }
    }
}
