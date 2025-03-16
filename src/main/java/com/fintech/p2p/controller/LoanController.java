package com.fintech.p2p.controller;

import com.fintech.p2p.model.Loan;
import com.fintech.p2p.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PreAuthorize("hasRole('BORROWER')") // 仅用于测试
    @PostMapping("/apply")
    public ResponseEntity<Loan> applyLoan(@RequestBody Loan loan) {
        Loan savedLoan = loanService.applyForLoan(loan);
        return ResponseEntity.ok(savedLoan);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Loan>> getPendingLoans() {
        return ResponseEntity.ok(loanService.getPendingLoans());
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveLoan(@PathVariable("id") Long id) {
        Optional<Loan> loan = loanService.approveLoan(id);
        return loan.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}