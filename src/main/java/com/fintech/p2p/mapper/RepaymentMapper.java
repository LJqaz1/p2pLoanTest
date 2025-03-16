package com.fintech.p2p.mapper;

import com.fintech.p2p.dto.RepaymentDTO;
import com.fintech.p2p.model.Repayment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RepaymentMapper {

    RepaymentDTO toDTO(Repayment repayment);

    List<RepaymentDTO> toDTOList(List<Repayment> repayments);
}