package com.fintech.p2p.mapper.impl;

import com.fintech.p2p.dto.RepaymentDTO;
import com.fintech.p2p.mapper.RepaymentMapper;
import com.fintech.p2p.model.Repayment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RepaymentMapperImpl implements RepaymentMapper {

    @Override
    public RepaymentDTO toDTO(Repayment repayment) {
        if (repayment == null) {
            return null;
        }

        RepaymentDTO dto = new RepaymentDTO();
        dto.setId(repayment.getId());
        dto.setLoanId(repayment.getLoanId());
        dto.setBorrowerId(repayment.getBorrowerId());
        dto.setAmount(repayment.getAmount());
        dto.setDueDate(repayment.getDueDate());
        dto.setStatus(repayment.getStatus());
        dto.setPaymentDate(repayment.getPaymentDate());

        return dto;
    }

    @Override
    public List<RepaymentDTO> toDTOList(List<Repayment> repayments) {
        if (repayments == null) {
            return null;
        }

        return repayments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}