package com.fintech.p2p.dto;

import com.fintech.p2p.enums.RepaymentStatus;
import com.fintech.p2p.enums.RepaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "还款计划DTO")
public class RepaymentDTO {
    @Schema(description = "还款ID")
    private Long id;

    @Schema(description = "贷款ID")
    private Long loanId;

    @Schema(description = "借款人ID")
    private Long borrowerId;

    @Schema(description = "借款人邮箱")
    private String borrowerEmail;

    @Schema(description = "还款金额")
    private BigDecimal amount;

    @Schema(description = "到期日期")
    private LocalDate dueDate;

    @Schema(description = "还款状态")
    private RepaymentStatus status;

    @Schema(description = "还款日期")
    private LocalDate paymentDate;

    @Schema(description = "还款时间戳")
    private LocalDateTime paymentTimestamp;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "还款类型")
    private RepaymentType repaymentType;
}
