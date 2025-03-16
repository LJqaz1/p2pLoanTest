package com.fintech.p2p.dto;

import com.fintech.p2p.enums.RepaymentStatus;
import com.fintech.p2p.enums.RepaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "还款计划创建请求")
public class RepaymentRequest {

    @NotNull(message = "贷款ID不能为空")
    @Schema(description = "贷款ID", required = true)
    private Long loanId;

    @NotBlank(message = "借款人邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "借款人邮箱", required = true)
    private String borrowerEmail;

    @NotNull(message = "借款人ID不能为空")
    @Schema(description = "借款人ID", required = true)
    private Long borrowerId;

    @NotNull(message = "还款金额不能为空")
    @Positive(message = "还款金额必须大于0")
    @Schema(description = "还款金额", required = true)
    private BigDecimal amount;

    @NotNull(message = "贷款状态不能为空")
    @Schema(description = "贷款状态", required = true)
    private RepaymentStatus status;

    @NotNull(message = "到期日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "到期日期 (格式: yyyy-MM-dd)", required = true, example = "2023-12-31")
    private LocalDate dueDate;

    @NotNull(message = "还款类型不能为空")
    @Schema(description = "还款类型", required = true)
    private RepaymentType repaymentType;
}
