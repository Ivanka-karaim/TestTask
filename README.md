# OpenBanking API

A Spring Boot REST API that simulates a PSD2-inspired OpenBanking service. Supports account balance retrieval, transaction history, and IBAN-to-IBAN payment initiation with OAuth2 authentication against a built-in mock bank.

---

## Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17, Spring Boot 3.3.5 |
| Persistence | Spring Data JPA, Hibernate, H2 (in-memory) |
| HTTP client | RestTemplate |
| Auth | OAuth2 Client Credentials (mocked) |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito, WireMock |
| Build | Maven |

---

## Getting started

### Prerequisites

- Java 17+
- Maven 3.8+

### Run

```bash
git clone <repo-url>
cd tesTasks

# Copy mock data to the expected location
mkdir -p data
cp src/main/resources/mock-data.json data/mock-data.json

mvn spring-boot:run
```

The application starts on **http://localhost:8084**.

### Run tests

```bash
mvn test
```

---

## API endpoints

### GET `/api/accounts/{accountId}/balance`

Returns the current balance for the given IBAN.

`accountId` must match `^UA[0-9]{27}$`.

**Example request**
```
GET /api/accounts/UA893220010000026005000000001/balance
```

**Example response** `200 OK`
```json
{
  "iban": "UA893220010000026005000000001",
  "balance": 5200.50,
  "currency": "EUR"
}
```

---

### GET `/api/accounts/{accountId}/transactions`

Returns up to 10 most recent transactions for the given IBAN, sorted newest first.

**Example request**
```
GET /api/accounts/UA893220010000026005000000001/transactions
```

**Example response** `200 OK`
```json
[
  {
    "externalId": "tx10",
    "amount": 99.99,
    "currency": "EUR",
    "timestamp": "2025-06-10T20:00:00Z",
    "description": "Clothes"
  }
]
```

---

### POST `/api/payments/initiate`

Initiates an IBAN-to-IBAN payment.

**Example request**
```json
{
  "fromIban": "UA893220010000026005000000001",
  "toIban":   "UA893220010000026005000000002",
  "amount":   250.00,
  "currency": "EUR"
}
```

**Example response** `200 OK`
```json
{
  "id": 1,
  "status": "COMPLETED",
  "externalReference": "ext-ref-abc123"
}
```

**Possible statuses:** `COMPLETED`, `FAILED`

---

### Error responses

All errors return a consistent JSON body:

```json
{
  "error": "Not Found",
  "message": "UA893220010000026005000000099 not found"
}
```

| HTTP status | When |
|---|---|
| `400 Bad Request` | Invalid IBAN format, non-positive amount, missing field, same source/destination IBAN |
| `404 Not Found` | Account does not exist |
| `409 Conflict` | Insufficient funds |
| `502 Bad Gateway` | External banking service unavailable |

---

## Payment state machine

```
PENDING → SENT → COMPLETED
                ↘ FAILED
```

| Status | Meaning |
|---|---|
| `PENDING` | Payment created and validated locally |
| `SENT` | Forwarded to external bank (committed to DB before the HTTP call) |
| `COMPLETED` | External bank confirmed success |
| `FAILED` | External bank rejected, or external call threw an exception |

The transition to `SENT` is committed to the database **before** the external HTTP call. This means that if the JVM crashes mid-flight, the record is recoverable — a scheduled job can query the external bank for any payments stuck in `SENT`.

---

## Architecture

```
controller/          REST layer — input validation, HTTP mapping
service/             Business logic — PaymentService, AccountService
external/            External bank adapter
  ExternalBankClient     RestTemplate calls to mock bank
  OAuthService           OAuth2 Client Credentials token cache
  ExternalErrorHandler   Maps HTTP error codes to domain exceptions
  MockBankController     Simulates external PSD2 bank (same process)
  MockBankDataService    File-based mock bank state (mock-data.json)
model/               JPA entities — Payment
repository/          Spring Data repositories
api/dto/             Request / response DTOs
exception/           Domain exceptions (NotFoundException, etc.)
config/              RestTemplate, Jackson configuration
```

### Mock bank

The mock bank runs inside the same Spring Boot process under `/mock/api`. It reads and writes state to `data/mock-data.json`. Both sides of a transfer are recorded: the source account gets a debit entry (negative amount), the destination account gets a credit entry (positive amount) if it exists in the mock data.

### OAuth2

The application implements the Client Credentials flow against the mock bank's `/mock/api/oauth/token` endpoint. Tokens are cached in memory and refreshed 30 seconds before expiry.

---

## Configuration

All settings live in `src/main/resources/application.properties`.

| Property | Default | Description |
|---|---|---|
| `server.port` | `8084` | HTTP port |
| `mock.file-path` | `./data/mock-data.json` | Path to mock bank JSON file |
| `external.bank.base-url` | `http://localhost:8084/mock/api` | Base URL of the external bank |
| `external.bank.client-id` | `test-client` | OAuth2 client ID |
| `external.bank.client-secret` | `test-secret` | OAuth2 client secret |
| `spring.h2.console.enabled` | `true` | Enable H2 web console |

---

## Useful URLs

| URL | Description |
|---|---|
| `http://localhost:8084/swagger-ui.html` | Swagger UI |
| `http://localhost:8084/api-docs` | OpenAPI JSON spec |
| `http://localhost:8084/h2-console` | H2 database console (JDBC URL: `jdbc:h2:mem:testdb`) |

---

## Mock accounts

Three accounts are seeded in `data/mock-data.json`:

| IBAN | Name | Balance |
|---|---|---|
| `UA893220010000026005000000001` | Ivanna | 5 200.50 EUR |
| `UA893220010000026005000000002` | Oleh | 8 400.00 EUR |
| `UA893220010000026005000000003` | Anna | 12 000.75 EUR |

---

## Testing

The project has three layers of tests:

**Unit tests** (`service/`) — `AccountServiceTest`, `PaymentServiceTest`. Use Mockito to isolate the service layer from the database and external HTTP calls.

**Integration tests** (`integration/IntegrationTests`) — full Spring Boot context with H2 and real HTTP via `TestRestTemplate`. WireMock replaces the external bank, so no real network calls are made. Covers all happy-path and error scenarios for all three endpoints.

**Smoke test** (`TesTasksApplicationTests`) — verifies the Spring context loads without errors.