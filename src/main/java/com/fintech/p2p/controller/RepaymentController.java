package com.fintech.p2p.controller;

import com.fintech.p2p.dto.RepaymentDTO;
import com.fintech.p2p.dto.RepaymentRequest;
import com.fintech.p2p.enums.RepaymentType;
import com.fintech.p2p.exception.InvalidRepaymentException;
import com.fintech.p2p.exception.ResourceNotFoundException;
import com.fintech.p2p.mapper.RepaymentMapper;
import com.fintech.p2p.model.Repayment;
import com.fintech.p2p.service.RepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/repayments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "还款管理", description = "还款计划创建与管理API")
public class RepaymentController {
    private final RepaymentService repaymentService;
    private final RepaymentMapper repaymentMapper;

    /**
     * 生成还款计划
     *
     * @param repaymentRequest 还款请求对象
     * @return 生成的还款记录
     */
    @PostMapping("/create")
    @Operation(summary = "创建还款计划", description = "根据贷款信息创建还款计划")
    @ApiResponse(responseCode = "201", description = "还款计划创建成功",
            content = @Content(schema = @Schema(implementation = RepaymentDTO.class)))
    @Tag(name = "还款管理", description = "还款计划创建与管理API")
    public ResponseEntity<RepaymentDTO> createRepayment(@Valid @RequestBody RepaymentRequest repaymentRequest) {
        Repayment repayment = repaymentService.createRepayment(
                repaymentRequest.getLoanId(),
                repaymentRequest.getBorrowerId(),
                repaymentRequest.getBorrowerEmail(),
                repaymentRequest.getAmount(),
                repaymentRequest.getDueDate(),
                repaymentRequest.getRepaymentType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(repaymentMapper.toDTO(repayment));
    }

    /**
     * 查询借款人的所有还款计划
     *
     * @param borrowerId 借款人ID
     * @return 还款记录列表
     */
    @GetMapping("/list/{borrowerId}")
    @Operation(summary = "获取借款人还款计划列表", description = "分页获取指定借款人的所有还款计划")
    public ResponseEntity<List<Repayment>> getRepaymentsByBorrower(@PathVariable Long borrowerId) {
        List<Repayment> repayments = repaymentService.getRepaymentsByBorrower(borrowerId);
        return ResponseEntity.ok(repayments);
    }

    /**
     * 计算部分还款
     *
     * @param request
     * @return
     */
    @PostMapping("/pay")
    @Operation(summary = "处理贷款还款", description = "接收还款请求并处理贷款还款")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "还款成功",
                    content = @Content(schema = @Schema(implementation = RepaymentDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    // 添加权限验证，确保只有贷款相关人员才能进行操作
//    @PreAuthorize("hasRole('USER') and @securityService.isBorrowerOrAdmin(#request.borrowerId)")
    @Tag(name = "还款管理", description = "还款计划创建与管理API")
    public ResponseEntity<RepaymentDTO> makeRepayment(@Valid @RequestBody RepaymentRequest request) {
        try {
            Repayment repayment = repaymentService.makeRepayment(
                    request.getLoanId(),
                    request.getBorrowerId(),
                    request.getBorrowerEmail(),
                    request.getAmount(),
                    request.getDueDate(),
                    request.getStatus(),
                    request.getRepaymentType()
            );
            RepaymentDTO repaymentDTO = repaymentMapper.toDTO(repayment);
            return ResponseEntity.ok(repaymentDTO);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidRepaymentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 进行还款
     *
     * @param repaymentId 还款记录ID
     * @return 更新后的还款记录
     */
    @PostMapping("/pay/{repaymentId}")
    @Operation(summary = "执行还款", description = "对指定的还款计划执行还款操作")
    @ApiResponse(responseCode = "200", description = "还款成功")
    @ApiResponse(responseCode = "404", description = "还款计划不存在")
    public ResponseEntity<RepaymentDTO> repay(@PathVariable Long repaymentId) {
        Optional<Repayment> repaymentOpt = repaymentService.repay(repaymentId);
        return repaymentOpt.map(r -> ResponseEntity.ok(repaymentMapper.toDTO(r)))
                .orElseThrow(() -> new ResourceNotFoundException("还款计划不存在: " + repaymentId));
    }

    /**
     * 获取待还款列表
     *
     * @param borrowerId 借款人ID
     * @return 待还款记录列表
     */
    @GetMapping("/borrower/{borrowerId}/pending")
    @Operation(summary = "获取待还款列表", description = "获取指定借款人的待还款计划")
    public ResponseEntity<List<RepaymentDTO>> getPendingRepayments(@PathVariable Long borrowerId) {
        List<Repayment> pendingRepayments = repaymentService.getPendingRepaymentsByBorrower(borrowerId);
        return ResponseEntity.ok(repaymentMapper.toDTOList(pendingRepayments));
    }

    /**
     * 根据日期范围查询还款计划
     *
     * @param borrowerId 借款人ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 符合条件的还款记录列表
     */
    @GetMapping("/borrower/{borrowerId}/date-range")
    @Operation(summary = "按日期范围查询还款计划", description = "获取指定借款人在日期范围内的还款计划")
    public ResponseEntity<List<RepaymentDTO>> getRepaymentsByDateRange(
            @PathVariable Long borrowerId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        List<Repayment> repayments = repaymentService.getRepaymentsByDateRange(borrowerId, startDate, endDate);
        return ResponseEntity.ok(repaymentMapper.toDTOList(repayments));
    }
}