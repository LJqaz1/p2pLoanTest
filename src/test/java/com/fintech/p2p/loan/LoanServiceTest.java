package com.fintech.p2p.loan;

import com.fintech.p2p.model.Loan;
import com.fintech.p2p.repository.LoanRepository;
import com.fintech.p2p.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {
    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanService loanService;

    private Loan loan;

    @BeforeEach
    void setUp() {
        loan = new Loan();
        loan.setBorrowerId(1L);
        loan.setAmount(BigDecimal.valueOf(10000));
        loan.setTerm(12);
        loan.setInterestRate(BigDecimal.valueOf(5.5));
    }

    @Test
    void testApplyForLoan() {
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        Loan savedLoan = loanService.applyForLoan(loan);
        assertNotNull(savedLoan);
        assertEquals(BigDecimal.valueOf(10000), savedLoan.getAmount());
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void testGetPendingLoans() {
        when(loanRepository.findByStatus(Loan.LoanStatus.PENDING)).thenReturn(List.of(loan));

        List<Loan> loans = loanService.getPendingLoans();
        assertFalse(loans.isEmpty());
    }
}