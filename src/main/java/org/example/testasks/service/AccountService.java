package org.example.testasks.service;

import lombok.RequiredArgsConstructor;
import org.example.testasks.api.dto.AccountBalanceResponse;
import org.example.testasks.api.dto.TransactionResponse;
import org.example.testasks.external.ExternalBankClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * Service layer responsible for retrieving account-related information
 * from the external banking system and mapping it into internal DTOs.
 *
 * <p>This service acts as a thin adapter between the internal API
 * and the external banking client.</p>
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final ExternalBankClient externalBankClient;
    /**
     * Retrieves account balance for the given IBAN.
     *
     * <p>Delegates the call to the external banking system and maps
     * the response into {@link AccountBalanceResponse}.</p>
     *
     * @param iban IBAN of the account
     * @return account balance information including amount and currency
     */
    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(String iban) {
        var ext = externalBankClient.getBalance(iban);
        return new AccountBalanceResponse(iban, ext.getBalance(), ext.getCurrency());
    }
    /**
     * Retrieves the latest transactions for the given IBAN.
     *
     * <p>Fetches all transactions from the external bank, sorts them
     * by timestamp in descending order, and returns only the latest 10.</p>
     *
     * <p>No persistence is performed; data is fetched in real-time.</p>
     *
     * @param iban IBAN of the account
     * @return list of up to 10 most recent transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(String iban) {
        var list = externalBankClient.getTransactions(iban);
        return list.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getAmount(),
                        t.getCurrency(),
                        t.getTimestamp(),
                        t.getDescription()
                ))
                .toList();
    }
}
