package org.example.testasks.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.example.testasks.api.dto.AccountBalanceResponse;
import org.example.testasks.api.dto.PaymentInitiateRequest;
import org.example.testasks.api.dto.PaymentInitiateResponse;
import org.example.testasks.api.dto.TransactionResponse;
import org.example.testasks.model.Payment;
import org.example.testasks.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the OpenBanking API.
 *
 * Strategy:
 *  - Real Spring Boot context + H2 + real HTTP (TestRestTemplate).
 *  - WireMock replaces external PSD2/mock-bank HTTP calls.
 *  - OAuth2 JWT validation disabled via application-test.properties
 *    (spring.autoconfigure.exclude), so no real Auth Server is needed.
 *  - paymentRepository.deleteAll() in @BeforeEach keeps tests isolated.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTests {

    private static final String IBAN_1 = "UA893220010000026005000000001";
    private static final String IBAN_2 = "UA893220010000026005000000002";
    private static final String IBAN_3 = "UA893220010000026005000000003";

    private static WireMockServer wireMock;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("external.bank.base-url",
                () -> "http://localhost:" + wireMock.port() + "/mock/api");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void resetState() {
        wireMock.resetAll();
        paymentRepository.deleteAll();
    }

    // ── GET /api/accounts/{accountId}/balance ─────────────────────────────

    @Test
    @Order(1)
    void getBalance_validIban_returnsBalance() {
        stubBalance(IBAN_1, 5200.50, "EUR");

        ResponseEntity<AccountBalanceResponse> response =
                restTemplate.getForEntity("/api/accounts/{iban}/balance",
                        AccountBalanceResponse.class, IBAN_1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AccountBalanceResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getIban()).isEqualTo(IBAN_1);
        assertThat(body.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(5200.50));
        assertThat(body.getCurrency()).isEqualTo("EUR");
    }

    @Test
    @Order(2)
    void getBalance_secondAccount_returnsCorrectBalance() {
        stubBalance(IBAN_2, 8400.00, "EUR");

        ResponseEntity<AccountBalanceResponse> response =
                restTemplate.getForEntity("/api/accounts/{iban}/balance",
                        AccountBalanceResponse.class, IBAN_2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(8400.00));
    }

    @Test
    @Order(3)
    void getBalance_invalidIbanFormat_returns400() {
        // IBAN doesn't match ^UA[0-9]{27}$ — validated before any HTTP call
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/accounts/{iban}/balance",
                        String.class, "INVALID_IBAN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void getBalance_externalServerReturns404_propagates5xx() {
        wireMock.stubFor(get(urlEqualTo("/mock/api/accounts/" + IBAN_1 + "/balance"))
                .willReturn(aResponse().withStatus(404)));

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/accounts/{iban}/balance",
                        String.class, IBAN_1);

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }

    // ── GET /api/accounts/{accountId}/transactions ────────────────────────

    @Test
    @Order(10)
    void getTransactions_moreThan10_returnsAtMost10() {
        stubTransactions(IBAN_1, buildTransactionsJson(IBAN_1, 12));

        ResponseEntity<TransactionResponse[]> response =
                restTemplate.getForEntity("/api/accounts/{iban}/transactions",
                        TransactionResponse[].class, IBAN_1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSizeLessThanOrEqualTo(10);
    }

    @Test
    @Order(11)
    void getTransactions_fewerThan10_returnsAll() {
        stubTransactions(IBAN_2, buildTransactionsJson(IBAN_2, 3));

        ResponseEntity<TransactionResponse[]> response =
                restTemplate.getForEntity("/api/accounts/{iban}/transactions",
                        TransactionResponse[].class, IBAN_2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(3);
    }

    @Test
    @Order(12)
    void getTransactions_invalidIban_returns400() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/accounts/{iban}/transactions",
                        String.class, "BAD-IBAN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(13)
    void getTransactions_sortedNewestFirst() {
        String json = """
                [
                  {"id":"old","iban":"%s","amount":10.00,"currency":"EUR",
                   "timestamp":"2025-01-01T10:00:00Z","description":"Old"},
                  {"id":"new","iban":"%s","amount":20.00,"currency":"EUR",
                   "timestamp":"2025-06-10T10:00:00Z","description":"New"}
                ]
                """.formatted(IBAN_1, IBAN_1);
        stubTransactions(IBAN_1, json);

        ResponseEntity<TransactionResponse[]> response =
                restTemplate.getForEntity("/api/accounts/{iban}/transactions",
                        TransactionResponse[].class, IBAN_1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TransactionResponse[] txs = response.getBody();
        assertThat(txs).isNotNull().hasSize(2);
        assertThat(txs[0].getExternalId()).isEqualTo("new");
        assertThat(txs[1].getExternalId()).isEqualTo("old");
    }

    // ── POST /api/payments/initiate ───────────────────────────────────────

    @Test
    @Order(20)
    void initiatePayment_success_completedAndSavedToDb() {
        stubBalance(IBAN_1, 1000.00, "EUR");
        stubPaymentCompleted("ext-ref-001");

        ResponseEntity<PaymentInitiateResponse> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, IBAN_2, 200.00, "EUR"),
                        PaymentInitiateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PaymentInitiateResponse resp = response.getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo("COMPLETED");
        assertThat(resp.getExternalReference()).isEqualTo("ext-ref-001");

        List<Payment> saved = paymentRepository.findAll();
        assertThat(saved).hasSize(1);
        Payment p = saved.get(0);
        assertThat(p.getStatus()).isEqualTo(Payment.Status.COMPLETED);
        assertThat(p.getFromIban()).isEqualTo(IBAN_1);
        assertThat(p.getToIban()).isEqualTo(IBAN_2);
        assertThat(p.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(p.getCurrency()).isEqualTo("EUR");
        assertThat(p.getExternalReference()).isEqualTo("ext-ref-001");
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    @Order(21)
    void initiatePayment_insufficientFunds_returns409_nothingSaved() {
        stubBalance(IBAN_1, 50.00, "EUR");

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, IBAN_2, 200.00, "EUR"),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    @Order(22)
    void initiatePayment_exactBalance_success() {
        stubBalance(IBAN_1, 500.00, "EUR");
        stubPaymentCompleted("ext-exact");

        ResponseEntity<PaymentInitiateResponse> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, IBAN_2, 500.00, "EUR"),
                        PaymentInitiateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @Order(23)
    void initiatePayment_externalBankReturnsFailed_savedAsFailed() {
        stubBalance(IBAN_1, 1000.00, "EUR");
        wireMock.stubFor(post(urlEqualTo("/mock/api/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"FAILED\",\"externalReference\":\"fail-007\"}")));

        ResponseEntity<PaymentInitiateResponse> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, IBAN_2, 100.00, "EUR"),
                        PaymentInitiateResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAILED");

        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(paymentRepository.findAll().get(0).getStatus()).isEqualTo(Payment.Status.FAILED);
    }

    @Test
    @Order(24)
    void initiatePayment_invalidFromIban_returns400() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest("WRONG", IBAN_2, 100.00, "EUR"),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    @Order(25)
    void initiatePayment_invalidToIban_returns400() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, "BAD_IBAN", 100.00, "EUR"),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(26)
    void initiatePayment_zeroAmount_returns400() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, IBAN_2, 0.00, "EUR"),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(27)
    void initiatePayment_negativeAmount_returns400() {
        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/payments/initiate",
                        buildPaymentRequest(IBAN_1, IBAN_2, -50.00, "EUR"),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(28)
    void initiatePayment_missingCurrency_returns400() {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setFromIban(IBAN_1);
        req.setToIban(IBAN_2);
        req.setAmount(BigDecimal.valueOf(100.00));
        // currency intentionally null

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/payments/initiate", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(29)
    void initiatePayment_multipleSuccessful_allPersisted() {
        stubBalance(IBAN_1, 5000.00, "EUR");
        stubPaymentCompleted("ext-m1");
        restTemplate.postForEntity("/api/payments/initiate",
                buildPaymentRequest(IBAN_1, IBAN_2, 100.00, "EUR"),
                PaymentInitiateResponse.class);

        wireMock.resetAll();
        stubBalance(IBAN_1, 4900.00, "EUR");
        stubPaymentCompleted("ext-m2");
        restTemplate.postForEntity("/api/payments/initiate",
                buildPaymentRequest(IBAN_1, IBAN_3, 300.00, "EUR"),
                PaymentInitiateResponse.class);

        assertThat(paymentRepository.findAll()).hasSize(2);
    }

    // ── WireMock helpers ──────────────────────────────────────────────────

    private void stubBalance(String iban, double balance, String currency) {
        wireMock.stubFor(get(urlEqualTo("/mock/api/accounts/" + iban + "/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"iban\":\"%s\",\"name\":\"Test\",\"balance\":%s,\"currency\":\"%s\"}"
                                .formatted(iban, balance, currency))));
    }

    private void stubTransactions(String iban, String txJson) {
        wireMock.stubFor(get(urlEqualTo("/mock/api/accounts/" + iban + "/transactions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(txJson)));
    }

    private void stubPaymentCompleted(String extRef) {
        wireMock.stubFor(post(urlEqualTo("/mock/api/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"COMPLETED\",\"externalReference\":\"%s\"}"
                                .formatted(extRef))));
    }

    // ── Domain helpers ────────────────────────────────────────────────────

    private PaymentInitiateRequest buildPaymentRequest(
            String from, String to, double amount, String currency) {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setFromIban(from);
        req.setToIban(to);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setCurrency(currency);
        return req;
    }

    /**
     * Builds a JSON array of {@code count} transactions for {@code iban}.
     * Timestamps increment by day so sort order is deterministic.
     */
    private String buildTransactionsJson(String iban, int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"tx").append(i)
                    .append("\",\"iban\":\"").append(iban)
                    .append("\",\"amount\":").append((i + 1) * 10)
                    .append(".00,\"currency\":\"EUR\",\"timestamp\":\"2025-06-")
                    .append(String.format("%02d", (i % 28) + 1))
                    .append("T10:00:00Z\",\"description\":\"tx-").append(i).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }
}
