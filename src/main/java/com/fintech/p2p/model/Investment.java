package com.fintech.p2p.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "investments")
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long investorId; // 投资人ID

    @Column(nullable = false)
    private Long loanId; // 贷款ID

    @Column(nullable = false)
    private BigDecimal amount; // 投资金额

    @Column(nullable = false)
    private String status; // PENDING / CONFIRMED

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
