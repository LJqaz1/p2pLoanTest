package com.fintech.p2p.repository;

import com.fintech.p2p.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByStatus(Loan.LoanStatus status);
}