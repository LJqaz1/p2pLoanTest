package com.fintech.p2p.repository;

import com.fintech.p2p.enums.RepaymentStatus;
import com.fintech.p2p.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    List<Repayment> findByLoanId(Long loanId);

    List<Repayment> findByBorrowerId(Long borrowerId);

    List<Repayment> findByDueDateBeforeAndStatus(LocalDate today, RepaymentStatus status);

    // 根据借款人ID和日期范围查询还款计划，并按到期日期排序
    List<Repayment> findByBorrowerIdAndDueDateBetweenOrderByDueDate(Long borrowerId, LocalDate startDate, LocalDate endDate);

    List<Repayment> findByBorrowerIdAndStatusOrderByDueDate(Long borrowerId, RepaymentStatus status);
}