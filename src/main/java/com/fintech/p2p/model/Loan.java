package com.fintech.p2p.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan", indexes = {
        @Index(name = "idx_loan_borrower", columnList = "borrower_id"),
        @Index(name = "idx_loan_status", columnList = "status")
})
@Data
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long borrowerId;

    @Column(nullable = false)
    private String borrowerEmail;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private int term; // 期限（月）

    @Column(nullable = false)
    private BigDecimal interestRate; // 利率

    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.PENDING; // 默认状态

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String purpose;
    private String description;

    @Enumerated(EnumType.STRING)
    private RepaymentStatus repaymentStatus;

    private BigDecimal repaidAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal remainingAmount;

    private Integer riskScore;
    private LocalDate fundingDeadline;
    private LocalDateTime updatedAt;

    public enum LoanStatus {
        PENDING, APPROVED, REJECTED, FUNDED, ACTIVE, COMPLETED, DEFAULTED
    }

    public enum RepaymentStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, DEFAULTED
    }

    @PrePersist
    public void prePersist() {
        if (this.remainingAmount == null) {
            this.remainingAmount = this.amount;
        }
    }
}