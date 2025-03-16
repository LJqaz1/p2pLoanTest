package com.fintech.p2p.service;

import com.fintech.p2p.enums.RepaymentStatus;
import com.fintech.p2p.enums.RepaymentType;
import com.fintech.p2p.exception.InvalidRepaymentException;
import com.fintech.p2p.model.Loan;
import com.fintech.p2p.model.Repayment;
import com.fintech.p2p.repository.LoanRepository;
import com.fintech.p2p.repository.RepaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentService {
    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationService notificationService;

    // 生成还款计划
    @Transactional
    public Repayment createRepayment(Long loanId, Long borrowerId, String borrowerEmail, BigDecimal amount, LocalDate dueDate, RepaymentType repaymentType) {
        if (loanId == null || borrowerId == null || borrowerEmail == null || amount == null || dueDate == null) {
            throw new IllegalArgumentException("所有参数都不能为空");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("还款金额必须大于0");
        }
        Repayment repayment = new Repayment();
        repayment.setLoanId(loanId);
        repayment.setBorrowerId(borrowerId);
        repayment.setBorrowerEmail(borrowerEmail);
        repayment.setAmount(amount);
        repayment.setDueDate(dueDate);
        repayment.setRepaymentType(repaymentType);
        repayment.setStatus(RepaymentStatus.PENDING); // 默认 PENDING 状态
        log.info("创建还款计划: 贷款ID={}, 借款人ID={}, 金额={}, 到期日={},还款类型={}",
                loanId, borrowerId, amount, dueDate, repayment);
        return repaymentRepository.save(repayment);
    }

    // 查询借款人的所有还款计划
    public List<Repayment> getRepaymentsByBorrower(Long borrowerId) {
        log.debug("查询借款人ID={}的还款计划", borrowerId);
        return repaymentRepository.findByBorrowerId(borrowerId);
    }

    /**
     * 查询贷款的所有还款计划
     */
    public List<Repayment> getRepaymentsByLoan(Long loanId) {
        log.debug("查询贷款ID={}的还款计划", loanId);
        return repaymentRepository.findByLoanId(loanId);
    }

    /**
     * 计算部分还款
     */
    @Transactional
    public Repayment makeRepayment(
            Long loanId,
            Long borrowerId,
            String borrowerEmail,
            BigDecimal amount,
            LocalDate dueDate,
            RepaymentStatus status,
            RepaymentType repaymentType) {
        // 参数验证
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("还款金额必须大于零");
        }

        // 获取贷款信息
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("贷款不存在,ID：" + loanId));

        // 验证借款人身份
        if (!loan.getBorrowerId().equals(borrowerId)) {
            throw new IllegalArgumentException("非法操作：该贷款不属于当前用户");
        }

        // 检查贷款状态
        if (loan.getStatus() == Loan.LoanStatus.COMPLETED) {
            throw new InvalidRepaymentException("该贷款已全部还清，无需再还款");
        }

        // 计算新的剩余本金
        BigDecimal remainingAmount = loan.getAmount().subtract(loan.getRepaidAmount());
        BigDecimal newRemainingAmount = remainingAmount.subtract(amount);

        // 验证还款金额是否合理
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("还款金额超过剩余本金");
        }

        // 创建还款记录
        Repayment repayment = new Repayment();
        repayment.setLoanId(loanId);
        repayment.setBorrowerId(borrowerId);
        repayment.setBorrowerEmail(borrowerEmail);
        repayment.setAmount(amount);
        repayment.setDueDate(dueDate);
        repayment.setStatus(status);
        repayment.setRepaymentType(repaymentType);
        repayment.setPaymentTimestamp(LocalDateTime.now()); // 添加还款时间

        // 保存还款记录
        repayment = repaymentRepository.save(repayment);
        log.info("已创建还款记录: ID={}, 金额={}, 类型={}",
                repayment.getId(), amount, repaymentType);

        // 更新贷款信息
        loan.setRepaidAmount(loan.getRepaidAmount().add(amount)); // 更新已还金额
        loan.setRemainingAmount(newRemainingAmount); // 更新剩余待还款金额

        // 更新贷款状态
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) == 0 || repaymentType == RepaymentType.FULL) {
            loan.setStatus(Loan.LoanStatus.COMPLETED);
            loan.setRepaymentStatus(Loan.RepaymentStatus.COMPLETED);
            log.info("贷款 ID={} 已全部还清", loanId);
        } else {
            loan.setRepaymentStatus(Loan.RepaymentStatus.IN_PROGRESS);
        }

        loan.setUpdatedAt(LocalDateTime.now()); // 更新修改时间
        loanRepository.save(loan);

        // 发送邮件通知 (异步)
        try {
            CompletableFuture<Boolean> emailFuture = notificationService.sendRepaymentNotification(
                    loan.getBorrowerEmail(),
                    loanId,
                    amount,
                    newRemainingAmount);
            // 异步结果（可选）
            emailFuture.thenAccept(success -> {
                if (!success) {
                    log.warn("发送还款通知邮件失败，贷款ID: {}, 借款人ID: {}", loanId, borrowerId);
                }
            });
        } catch (Exception e) {
            // 记录日志但不影响事务
            log.error("发送还款通知邮件时发生错误: {}", e.getMessage(), e);
        }
        return repayment;
    }

    // 借款人还款
    public Optional<Repayment> repay(Long repaymentId) {
        log.info("处理还款ID={}", repaymentId);
        Optional<Repayment> repaymentOpt = repaymentRepository.findById(repaymentId);
        if (repaymentOpt.isEmpty()) {
            log.warn("找不到还款ID={}", repaymentId);
            return Optional.empty();
        }
        Repayment repayment = repaymentOpt.get();

        if (repayment.getStatus() == RepaymentStatus.PAID) {
            log.warn("还款ID={}已支付，无需重复操作", repaymentId);
            return repaymentOpt;
        }

        repayment.setStatus(RepaymentStatus.PAID);
        repayment.setPaymentDate(LocalDate.now());
        repayment.setPaymentTimestamp(LocalDateTime.now());

        repaymentRepository.save(repayment);

        log.info("还款ID={}已成功处理，状态更新为PAID", repaymentId);

        return Optional.of(repayment);
    }

    /**
     * 每天检查逾期还款（凌晨 00:00 运行）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional(readOnly = true)
    public void checkOverdueRepayments() {
        log.info("开始执行逾期还款检查任务");
        LocalDate today = LocalDate.now();
        List<Repayment> overdueRepayments = repaymentRepository.findByDueDateBeforeAndStatus(today, RepaymentStatus.PENDING);

        log.info("发现{}笔逾期还款", overdueRepayments.size());
        for (Repayment repayment : overdueRepayments) {
            // 更新状态为逾期
            repayment.setStatus(RepaymentStatus.OVERDUE);
            repaymentRepository.save(repayment);

            String borrowerEmail = repayment.getBorrowerEmail();
            String loanId = repayment.getLoanId().toString();
            BigDecimal amount = repayment.getAmount();

            if (borrowerEmail == null || borrowerEmail.isEmpty()) {
                log.error("还款ID={}的借款人邮箱为空，无法发送通知", repayment.getId());
                continue;
            }

            String message = String.format("%s,%s,%.2f", borrowerEmail, loanId, amount);
            log.debug("发送Kafka消息: {}", message);

            // 发送 Kafka 消息到 NotificationService
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send("overdue-repayment-topic", message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("成功发送消息到Kafka: {}", message);
                } else {
                    log.error("发送消息到Kafka失败: {}", ex.getMessage());
                }
            });
        }
        log.info("逾期还款检查任务执行完毕");
    }

    /**
     * 手动标记还款为逾期状态
     */
    @Transactional
    public boolean markAsOverdue(Long repaymentId) {
        Optional<Repayment> repaymentOpt = repaymentRepository.findById(repaymentId);
        if (repaymentOpt.isEmpty()) {
            return false;
        }

        Repayment repayment = repaymentOpt.get();
        if (!RepaymentStatus.PENDING.equals(repayment.getStatus())) {
            return false;
        }

        repayment.setStatus(RepaymentStatus.OVERDUE);
        repaymentRepository.save(repayment);

        // 发送通知
        String message = String.format("%s,%s,%.2f",
                repayment.getBorrowerEmail(),
                repayment.getLoanId(),
                repayment.getAmount());

        kafkaTemplate.send("overdue-repayment-topic", message);

        return true;
    }

    public List<Repayment> getRepaymentsByDateRange(Long borrowerId, LocalDate startDate, LocalDate endDate) {
        if (borrowerId == null) {
            throw new IllegalArgumentException("借款人ID不能为空");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        return repaymentRepository.findByBorrowerIdAndDueDateBetweenOrderByDueDate(
                borrowerId,
                startDate,
                endDate
        );
    }

    /**
     * 获取借款人的所有待还款计划
     *
     * @param borrowerId 借款人ID
     * @return 待还款的还款计划列表
     */
    public List<Repayment> getPendingRepaymentsByBorrower(Long borrowerId) {
        if (borrowerId == null) {
            throw new IllegalArgumentException("借款人ID不能为空");
        }

        // 假设还款状态"PENDING"表示待还款
        return repaymentRepository.findByBorrowerIdAndStatusOrderByDueDate(
                borrowerId,
                RepaymentStatus.PENDING
        );
    }
}