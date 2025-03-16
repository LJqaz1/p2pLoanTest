package com.fintech.p2p.repository;

import com.fintech.p2p.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByInvestorId(Long investorId);
    List<Investment> findByLoanId(Long loanId);
}
