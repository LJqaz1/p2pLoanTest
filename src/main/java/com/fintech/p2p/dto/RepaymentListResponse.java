package com.fintech.p2p.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "还款计划分页响应")
public class RepaymentListResponse {
    @Schema(description = "还款计划列表")
    private List<RepaymentDTO> repayments;

    @Schema(description = "当前页码")
    private int pageNumber;

    @Schema(description = "每页大小")
    private int pageSize;

    @Schema(description = "总记录数")
    private long totalElements;

    @Schema(description = "总页数")
    private int totalPages;
}