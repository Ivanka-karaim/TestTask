package org.example.testasks.service;

import org.example.testasks.api.dto.AccountBalanceResponse;
import org.example.testasks.api.dto.TransactionResponse;
import org.example.testasks.external.ExternalBankClient;
import org.example.testasks.external.dto.ExternalBalanceDto;
import org.example.testasks.external.dto.ExternalTransactionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    ExternalBankClient externalBankClient;


    @InjectMocks
    AccountService accountService;

    @Test
    void getBalance_returnsMappedBalance() {

        String iban = "UA893220010000026005000000031";

        when(externalBankClient.getBalance(iban))
                .thenReturn(new ExternalBalanceDto(
                        iban,
                        "Ivanna",
                        BigDecimal.valueOf(1234),
                        "EUR"
                ));

        AccountBalanceResponse resp = accountService.getBalance(iban);

        assertNotNull(resp);
        assertEquals(iban, resp.getIban());
        assertEquals(BigDecimal.valueOf(1234), resp.getBalance());
        assertEquals("EUR", resp.getCurrency());
    }

    @Test
    void getTransactions_returnsMappedTransactions() {

        String iban = "UA893220010000026005000000021";

        ExternalTransactionDto t1 = new ExternalTransactionDto(
                "id1",
                iban,
                BigDecimal.valueOf(10),
                "EUR",
                OffsetDateTime.now(),
                "t1"
        );

        ExternalTransactionDto t2 = new ExternalTransactionDto(
                "id2",
                iban,
                BigDecimal.valueOf(20),
                "EUR",
                OffsetDateTime.now().minusDays(1),
                "t2"
        );

        when(externalBankClient.getTransactions(iban))
                .thenReturn(List.of(t1, t2));

        List<TransactionResponse> resp = accountService.getTransactions(iban);

        assertNotNull(resp);
        assertEquals(2, resp.size());

        assertEquals("id1", resp.get(0).getExternalId());
        assertEquals(BigDecimal.valueOf(10), resp.get(0).getAmount());

        assertEquals("id2", resp.get(1).getExternalId());
    }
}

