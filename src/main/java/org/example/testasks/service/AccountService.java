package org.example.testasks.service;

import lombok.RequiredArgsConstructor;
import org.example.testasks.api.dto.AccountBalanceResponse;
import org.example.testasks.api.dto.TransactionResponse;
import org.example.testasks.external.ExternalBankClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final ExternalBankClient externalBankClient;


    @Transactional
    public AccountBalanceResponse getBalance(String iban) {
        var ext = externalBankClient.getBalance(iban);
        return new AccountBalanceResponse(iban, ext.getBalance(), ext.getCurrency());
    }

    @Transactional
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
