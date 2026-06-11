package org.example.testasks.external.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MockBankDataDto {
    private List<MockAccountDto> accounts;
}