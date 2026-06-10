package org.example.testasks.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromIban;

    private String toIban;

    private BigDecimal amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private Status status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private String externalReference;

    public enum Status {
        PENDING, SENT, COMPLETED, FAILED
    }
}
