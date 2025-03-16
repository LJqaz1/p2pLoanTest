package com.fintech.p2p.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvestmentRequest {
    private Long investorId;
    private Long loanId;
    private BigDecimal amount;
}
