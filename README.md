# tesTasks — OpenBanking test task

Локальний Spring Boot застосунок для тестового завдання OpenBanking.

## Вимоги
- Java 17+
- Maven

## Запуск локально

Запустити додаток у режимі розробки:

```bash
mvn spring-boot:run
```

API доки (Swagger UI) будуть доступні за:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI spec також збережено як `src/main/resources/openapi.yaml`.

H2 консоль:

```
http://localhost:8080/h2-console
```

## OAuth2 (опціонально)
Щоб використовувати OAuth2 client credentials для викликів до зовнішнього банку, розкоментуйте і заповніть секцію у `src/main/resources/application.properties`:

```
#spring.security.oauth2.client.registration.external-bank.client-id=your-client-id
#spring.security.oauth2.client.registration.external-bank.client-secret=your-client-secret
#spring.security.oauth2.client.registration.external-bank.authorization-grant-type=client_credentials
#spring.security.oauth2.client.registration.external-bank.scope=payments accounts
#spring.security.oauth2.client.provider.external-bank.token-uri=https://auth.example.com/oauth2/token
```

Якщо ці властивості задані, конфігурація створить OAuth2-enabled `WebClient` автоматично.

## Тести
- Запустити юніт + інтеграційні тести:

```bash
mvn test
```

Інтеграційні тести використовують WireMock для заглушення зовнішнього PSD2 API.

## Що зроблено
- REST API: баланс, транзакції, ініціація платежу
- Локальний мок PSD2 (`/mock/api`)
- Збереження платежів у H2
- Валідація, глобальний обробник помилок
- Unit tests (Mockito) і Integration tests (WireMock)
- OpenAPI/Swagger
- Optional OAuth2 client credentials support for external calls

---

Якщо хочеш, наступним кроком можу:
- Додати більше тестів покриття або
- Провести рефакторинг/документування контрактів OpenAPI або
- Налаштувати CI (GitHub Actions) для запуску тестів.
