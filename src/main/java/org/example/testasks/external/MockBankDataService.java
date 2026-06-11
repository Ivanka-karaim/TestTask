package org.example.testasks.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.testasks.exception.ConflictException;
import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.dto.ExternalTransactionDto;
import org.example.testasks.external.dto.MockAccountDto;
import org.example.testasks.external.dto.MockBankDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
/**
 * Mock implementation of a banking data source using a local JSON file.
 *
 * <p>This service simulates a simple banking system by reading and writing
 * account data directly from/to a JSON file.</p>
 *
 * <h3>Important characteristics:</h3>
 * <ul>
 *   <li>File-based persistence (no real database)</li>
 *   <li>Non-thread-safe (no locking mechanism)</li>
 *   <li>Used for development / testing purposes only</li>
 * </ul>
 *
 * <p><b>Warning:</b> Concurrent access may lead to race conditions and data loss.</p>
 */

@Service
@RequiredArgsConstructor
public class MockBankDataService {

    private final ObjectMapper objectMapper;
    /**
     * Path to JSON file containing mock bank data.
     */
    @Value("${mock.file-path}")
    private String filePath;
    /**
     * Retrieves a bank account by IBAN.
     *
     * @param iban account identifier
     * @return account data if found
     * @throws NotFoundException if account does not exist
     */
    public MockAccountDto getAccount(String iban) {
        MockBankDataDto data = readFile();

        return data.getAccounts()
                .stream()
                .filter(a -> a.getIban().equals(iban))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(iban + " not found"));
    }
    /**
     * Simulates an IBAN-to-IBAN payment between two accounts.
     *
     * <p>Both sides of the transfer are recorded:</p>
     * <ul>
     *   <li>Source account — balance decremented, debit transaction added (negative amount)</li>
     *   <li>Destination account — balance incremented, credit transaction added (positive amount).
     *       If {@code toIban} is not found in the mock data, the credit side is silently skipped
     *       (simulates a transfer to an external bank outside this mock).</li>
     * </ul>
     *
     * <p>Both accounts are updated atomically within a single file write.</p>
     *
     * @param fromIban source account IBAN
     * @param toIban   destination account IBAN
     * @param amount   transfer amount (positive)
     * @param currency ISO 4217 currency code
     * @return generated transaction ID (shared by both legs of the transfer)
     * @throws NotFoundException   if source account is not found
     * @throws ConflictException   if source account has insufficient funds
     */
    public synchronized String initiatePayment(
            String fromIban, String toIban, BigDecimal amount, String currency) {

        MockBankDataDto data = readFile();

        MockAccountDto fromAccount = findAccount(data, fromIban);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ConflictException("Insufficient funds");
        }

        String txId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();

        // Debit leg: subtract from source, record as negative amount
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        fromAccount.getTransactions().add(new ExternalTransactionDto(
                txId,
                toIban,
                amount.negate(),
                currency,
                now,
                "Payment to " + toIban
        ));

        // Credit leg: add to destination if it exists in this mock bank
        data.getAccounts().stream()
                .filter(a -> a.getIban().equals(toIban))
                .findFirst()
                .ifPresent(toAccount -> {
                    toAccount.setBalance(toAccount.getBalance().add(amount));
                    toAccount.getTransactions().add(new ExternalTransactionDto(
                            txId,
                            fromIban,
                            amount,
                            currency,
                            now,
                            "Payment from " + fromIban
                    ));
                });

        writeFile(data);

        return txId;
    }

    private MockAccountDto findAccount(MockBankDataDto data, String iban) {
        return data.getAccounts().stream()
                .filter(a -> a.getIban().equals(iban))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(iban + " not found"));
    }

    /**
     * Reads mock banking data from JSON file.
     *
     * @return deserialized banking data
     * @throws RuntimeException if file cannot be read or parsed
     */
    private MockBankDataDto readFile() {
        try (InputStream is = new FileInputStream(filePath)) {
            return objectMapper.readValue(is, MockBankDataDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Writes updated banking data back to JSON file.
     *
     * @param data updated bank state
     * @throws RuntimeException if file cannot be written
     */
    private void writeFile(MockBankDataDto data) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filePath), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}