package org.example.testasks.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.dto.ExternalTransactionDto;
import org.example.testasks.external.dto.MockAccountDto;
import org.example.testasks.external.dto.MockBankDataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockBankDataService {

    private final ObjectMapper objectMapper;

    @Value("${mock.file-path}")
    private String filePath;

    public MockAccountDto getAccount(String iban) {
        MockBankDataDto data = readFile();

        return data.getAccounts()
                .stream()
                .filter(a -> a.getIban().equals(iban))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(iban + " not found"));
    }

    // 👉 NEW: initiate transaction in mock JSON
    public String initiatePayment(String fromIban, String toIban, BigDecimal amount, String currency) {

        MockBankDataDto data = readFile();

        MockAccountDto fromAccount = data.getAccounts().stream()
                .filter(a -> a.getIban().equals(fromIban))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(fromIban + " not found"));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // decrease balance
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));

        // create transaction
        ExternalTransactionDto tx = new ExternalTransactionDto(
                UUID.randomUUID().toString(),
                fromIban,
                amount,
                currency,
                OffsetDateTime.now(),
                "Payment to " + toIban
        );

        fromAccount.getTransactions().add(tx);

        writeFile(data);

        return tx.getId();
    }

    // ---------------- helpers ----------------

    private MockBankDataDto readFile() {
        try (InputStream is = new FileInputStream(filePath)) {
            return objectMapper.readValue(is, MockBankDataDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFile(MockBankDataDto data) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filePath), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}