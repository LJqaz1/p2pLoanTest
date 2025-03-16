package com.fintech.p2p.service;

import com.fintech.p2p.model.Loan;
import com.fintech.p2p.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LoanService {
    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Transactional
    public Loan applyForLoan(Loan loan) {
        loan.setStatus(Loan.LoanStatus.PENDING);
        return loanRepository.save(loan);
    }

    public List<Loan> getPendingLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.PENDING);
    }

    @Transactional
    public Optional<Loan> approveLoan(Long loanId) {
        Optional<Loan> loan = loanRepository.findById(loanId);
        loan.ifPresent(l -> {
            l.setStatus(Loan.LoanStatus.APPROVED);
            loanRepository.save(l);
        });
        return loan;
    }
}