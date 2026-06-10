package org.example.testasks.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.example.testasks.api.dto.AccountBalanceResponse;
import org.example.testasks.api.dto.PaymentInitiateRequest;
import org.example.testasks.api.dto.PaymentInitiateResponse;
import org.example.testasks.model.Payment;
import org.example.testasks.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PaymentRepository paymentRepository;

    private static WireMockServer wireMockServer;

    private static final String IBAN_1 = "UA893220010000026005000000001";
    private static final String IBAN_2 = "UA893220010000026005000000002";

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().dynamicPort()
        );
        wireMockServer.start();

        configureFor("localhost", wireMockServer.port());
    }
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("external.bank.base-url",
                () -> "http://localhost:" + wireMockServer.port() + "/mock/api");
    }

    @AfterAll
    static void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void getBalance_success_returnsDataFromExternalBank() {

        stubFor(get(urlEqualTo("/mock/api/accounts/" + IBAN_1 + "/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "iban": "UA893220010000026005000000001",
                                  "name": "Ivanna",
                                  "balance": 5200.50,
                                  "currency": "EUR"
                                }
                                """)));

        webTestClient.get()
                .uri("/api/accounts/" + IBAN_1 + "/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountBalanceResponse.class)
                .value(body -> {
                    assertNotNull(body);
                    assertEquals(IBAN_1, body.getIban());
                    assertEquals(0,
                            BigDecimal.valueOf(5200.50).compareTo(body.getBalance()));
                    assertEquals("EUR", body.getCurrency());
                });
    }

    @Test
    void initiatePayment_success_savesToDbAndReturnsCompleted() {

        BigDecimal amount = BigDecimal.valueOf(200.00);

        stubFor(get(urlEqualTo("/mock/api/accounts/" + IBAN_1 + "/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "iban": "UA893220010000026005000000001",
                                  "balance": 1000.00,
                                  "currency": "EUR"
                                }
                                """)));

        stubFor(post(urlEqualTo("/mock/api/payments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status": "COMPLETED",
                                  "externalReference": "ext-ref-999"
                                }
                                """)));

        PaymentInitiateRequest request = new PaymentInitiateRequest();
        request.setFromIban(IBAN_1);
        request.setToIban(IBAN_2);
        request.setAmount(amount);
        request.setCurrency("EUR");

        webTestClient.post()
                .uri("/api/payments/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentInitiateResponse.class)
                .value(body -> {
                    assertNotNull(body);
                    assertEquals("COMPLETED", body.getStatus());
                    assertEquals("ext-ref-999", body.getExternalReference());
                });

        List<Payment> saved = paymentRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(Payment.Status.COMPLETED, saved.get(0).getStatus());
    }
}