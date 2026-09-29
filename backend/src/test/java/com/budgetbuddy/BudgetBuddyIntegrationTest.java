package com.budgetbuddy;

import com.budgetbuddy.chat.ChatRequest;
import com.budgetbuddy.recurring.RecurringFinanceRequest;
import com.budgetbuddy.transaction.TransactionRequest;
import com.budgetbuddy.transaction.TransactionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-context integration tests against H2 (MySQL mode) covering:
 * registration/login, JWT protection, transaction CRUD + ownership
 * isolation, recurring generation idempotency, dashboard aggregates,
 * budget warning, savings target and chat validation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "server.port=18099")
class BudgetBuddyIntegrationTest {

    private static final String BASE = "http://localhost:18099";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private RestClient client() {
        return RestClient.builder().baseUrl(BASE).build();
    }

    /** Runs a call and converts any error status into a normal ResponseEntity. */
    private ResponseEntity<String> call(Supplier<ResponseEntity<String>> fn) {
        try {
            return fn.get();
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private String registerAndGetToken(String email) {
        ResponseEntity<String> resp = call(() -> client().post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "User " + email, "email", email, "password", "Str0ngPass!x"))
                .retrieve().toEntity(String.class));
        assertThat(resp.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.CREATED);
        return extract(resp, "token");
    }

    private RestClient withAuth(String token) {
        return RestClient.builder()
                .baseUrl(BASE)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    private String extract(ResponseEntity<String> resp, String field) {
        try {
            return objectMapper.readTree(resp.getBody()).get(field).asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int countTransactions(String pageJson) {
        return parse(pageJson).get("totalElements").asInt();
    }

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<String> resp = call(() -> client().get()
                .uri("/actuator/health").retrieve().toEntity(String.class));
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void protectedEndpointRejectsAnonymous() {
        ResponseEntity<String> resp = call(() -> client().get()
                .uri("/api/transactions").retrieve().toEntity(String.class));
        assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void registerAndLoginWork() {
        String token = registerAndGetToken("alice@example.com");
        assertThat(token).isNotBlank();

        ResponseEntity<String> me = call(() -> withAuth(token).get()
                .uri("/api/users/me").retrieve().toEntity(String.class));
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(extract(me, "email")).isEqualTo("alice@example.com");
    }

    @Test
    void duplicateEmailIsRejected() {
        registerAndGetToken("bob@example.com");
        ResponseEntity<String> resp = call(() -> client().post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "Bob 2", "email", "bob@example.com", "password", "Str0ngPass!x"))
                .retrieve().toEntity(String.class));
        assertThat(resp.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void wrongPasswordIsRejected() {
        registerAndGetToken("carol@example.com");
        ResponseEntity<String> resp = call(() -> client().post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", "carol@example.com", "password", "WrongPass!123"))
                .retrieve().toEntity(String.class));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void transactionCrudAndIsolation() {
        String tokenA = registerAndGetToken("txa@example.com");
        String tokenB = registerAndGetToken("txb@example.com");

        TransactionRequest create = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("1250.00"),
                LocalDate.of(2026, 9, 15), "Food", "Dinner");
        ResponseEntity<String> created = call(() -> withAuth(tokenA).post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(create)
                .retrieve().toEntity(String.class));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String txId = extract(created, "id");

        // Owner can read it.
        ResponseEntity<String> owned = call(() -> withAuth(tokenA).get()
                .uri("/api/transactions/" + txId).retrieve().toEntity(String.class));
        assertThat(owned.getStatusCode().value()).isEqualTo(200);

        // Another user must not see it.
        ResponseEntity<String> foreign = call(() -> withAuth(tokenB).get()
                .uri("/api/transactions/" + txId).retrieve().toEntity(String.class));
        assertThat(foreign.getStatusCode().value()).isEqualTo(404);

        // Another user must not be able to delete it.
        ResponseEntity<String> foreignDelete = call(() -> withAuth(tokenB)
                .method(HttpMethod.DELETE).uri("/api/transactions/" + txId)
                .retrieve().toEntity(String.class));
        assertThat(foreignDelete.getStatusCode().value()).isEqualTo(404);

        // Update then delete as owner.
        TransactionRequest update = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("1300.50"),
                LocalDate.of(2026, 9, 16), "Food", "Updated dinner");
        ResponseEntity<String> updated = call(() -> withAuth(tokenA).put()
                .uri("/api/transactions/" + txId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .retrieve().toEntity(String.class));
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(extract(updated, "amount")).isEqualTo("1300.5");

        ResponseEntity<String> deleted = call(() -> withAuth(tokenA)
                .method(HttpMethod.DELETE).uri("/api/transactions/" + txId)
                .retrieve().toEntity(String.class));
        assertThat(deleted.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> gone = call(() -> withAuth(tokenA).get()
                .uri("/api/transactions/" + txId).retrieve().toEntity(String.class));
        assertThat(gone.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void transactionValidationRejectsBadAmount() {
        String token = registerAndGetToken("val@example.com");
        TransactionRequest invalid = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("0.00"),
                LocalDate.of(2026, 9, 15), "Food", null);
        ResponseEntity<String> resp = call(() -> withAuth(token).post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalid)
                .retrieve().toEntity(String.class));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void recurringGenerationIsIdempotentAndFutureOnly() {
        String token = registerAndGetToken("rec@example.com");

        // Start on day 1 of the month two months back -> catch-up should
        // create 3 periods (two past months + the current month).
        LocalDate start = YearMonth.now().minusMonths(2).atDay(1);
        RecurringFinanceRequest request = new RecurringFinanceRequest(
                TransactionType.INCOME, new BigDecimal("50000.00"), "Salary",
                start, null, "Monthly salary", true);
        ResponseEntity<String> created = call(() -> withAuth(token).post()
                .uri("/api/recurring-finances")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve().toEntity(String.class));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        String configId = extract(created, "id");

        // Verify generated transactions (start day 1 always passed -> 3 periods).
        ResponseEntity<String> txs = call(() -> withAuth(token).get()
                .uri("/api/transactions?type=INCOME&category=Salary&size=50")
                .retrieve().toEntity(String.class));
        assertThat(txs.getStatusCode().value()).isEqualTo(200);
        int firstRunCount = countTransactions(txs.getBody());
        assertThat(firstRunCount).isEqualTo(3);

        // Trigger another generation pass; it must not duplicate anything.
        var generationService = applicationContext.getBean(com.budgetbuddy.recurring.RecurringGenerationService.class);
        generationService.generateForAllUsers();

        ResponseEntity<String> txsAgain = call(() -> withAuth(token).get()
                .uri("/api/transactions?type=INCOME&category=Salary&size=50")
                .retrieve().toEntity(String.class));
        int secondRunCount = countTransactions(txsAgain.getBody());
        assertThat(secondRunCount).as("generation must be idempotent").isEqualTo(firstRunCount);

        // Change the recurring amount: allowed; history stays untouched.
        RecurringFinanceRequest update = new RecurringFinanceRequest(
                TransactionType.INCOME, new BigDecimal("55000.00"), "Salary",
                start, null, "Monthly salary", true);
        ResponseEntity<String> updated = call(() -> withAuth(token).put()
                .uri("/api/recurring-finances/" + configId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .retrieve().toEntity(String.class));
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(extract(updated, "amount")).isEqualTo("55000.0");
    }

    @Test
    void dashboardReturnsAggregates() {
        String token = registerAndGetToken("dash@example.com");

        TransactionRequest income = new TransactionRequest(
                TransactionType.INCOME, new BigDecimal("60000.00"),
                LocalDate.now(), "Salary", null);
        TransactionRequest expense1 = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("20000.00"),
                LocalDate.now(), "Rent", null);
        TransactionRequest expense2 = new TransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("15000.00"),
                LocalDate.now(), "Food", null);
        call(() -> withAuth(token).post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON).body(income).retrieve().toEntity(String.class));
        call(() -> withAuth(token).post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON).body(expense1).retrieve().toEntity(String.class));
        call(() -> withAuth(token).post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON).body(expense2).retrieve().toEntity(String.class));

        // Budget under expenses triggers the warning; savings target for progress.
        Map<String, Object> profile = Map.of(
                "monthlyBudget", 30000,
                "monthlySavingsTarget", 10000);
        ResponseEntity<String> updated = call(() -> withAuth(token).put()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(profile)
                .retrieve().toEntity(String.class));
        assertThat(updated.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> dash = call(() -> withAuth(token).get()
                .uri("/api/dashboard").retrieve().toEntity(String.class));
        assertThat(dash.getStatusCode().value()).isEqualTo(200);
        JsonNode node = parse(dash.getBody());
        assertThat(node.get("currentMonth").get("income").asDouble()).isEqualTo(60000.0);
        assertThat(node.get("currentMonth").get("expense").asDouble()).isEqualTo(35000.0);
        assertThat(node.get("currentMonth").get("net").asDouble()).isEqualTo(25000.0);
        assertThat(node.get("budgetInfo").get("overBudget").asBoolean()).isTrue();
        assertThat(node.get("budgetInfo").get("exceededBy").asDouble()).isEqualTo(5000.0);
        assertThat(node.get("savingsTargetInfo").get("progressPercent").asDouble()).isEqualTo(250.0);
        assertThat(node.get("monthlySeries").size()).isEqualTo(12);
        assertThat(node.get("categoryBreakdown").size()).isGreaterThanOrEqualTo(2);
        assertThat(node.get("recentTransactions").size()).isEqualTo(3);
    }

    @Test
    void chatRequiresAuthenticationAndValidMessage() {
        ChatRequest empty = new ChatRequest("");
        ResponseEntity<String> noAuth = call(() -> client().post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(empty)
                .retrieve().toEntity(String.class));
        assertThat(noAuth.getStatusCode().value()).isEqualTo(401);

        String token = registerAndGetToken("chat@example.com");
        ResponseEntity<String> blank = call(() -> withAuth(token).post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest("   "))
                .retrieve().toEntity(String.class));
        assertThat(blank.getStatusCode().value()).isEqualTo(400);

        // With a non-production test key the upstream call fails and maps to
        // 502/503; the important part is that auth worked (not 401/403).
        ResponseEntity<String> send = call(() -> withAuth(token).post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest("How do I save more?"))
                .retrieve().toEntity(String.class));
        assertThat(send.getStatusCode().value()).isIn(200, 502, 503);
    }

    @Test
    void crossUserDashboardIsolation() {
        String tokenA = registerAndGetToken("isoA@example.com");
        String tokenB = registerAndGetToken("isoB@example.com");

        TransactionRequest income = new TransactionRequest(
                TransactionType.INCOME, new BigDecimal("1000.00"),
                LocalDate.now(), "Other", null);
        call(() -> withAuth(tokenA).post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON).body(income).retrieve().toEntity(String.class));

        ResponseEntity<String> dashB = call(() -> withAuth(tokenB).get()
                .uri("/api/dashboard").retrieve().toEntity(String.class));
        JsonNode node = parse(dashB.getBody());
        assertThat(node.get("currentMonth").get("income").asDouble()).isEqualTo(0.0);
        assertThat(node.get("currentMonth").get("expense").asDouble()).isEqualTo(0.0);
    }
}
