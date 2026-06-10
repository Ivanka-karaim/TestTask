package org.example.testasks.external.dto;

import lombok.Data;
import java.util.List;

@Data
public class MockBankDataDto {
    private List<MockAccountDto> accounts;
}