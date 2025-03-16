package com.fintech.p2p.model;

import com.fintech.p2p.enums.RepaymentStatus;
import com.fintech.p2p.enums.RepaymentType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "repayments")
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId; // 贷款ID

    @Column(nullable = false)
    private Long borrowerId; // 借款人ID

    @Column(nullable = false)
    private String borrowerEmail;

    @Column(nullable = false)
    private BigDecimal amount; // 还款金额

    @Column(nullable = false)
    private LocalDate dueDate; // 还款截止日期

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentStatus status;

    // 实际支付日期（业务日期）
    private LocalDate paymentDate;

    // 实际支付时间戳（精确到秒/毫秒）
    private LocalDateTime paymentTimestamp;

    // 记录创建时间
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 记录最后更新时间
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 还款类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentType repaymentType;
}