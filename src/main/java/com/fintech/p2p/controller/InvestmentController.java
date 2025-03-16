package com.fintech.p2p.controller;

import com.fintech.p2p.dto.InvestmentRequest;
import com.fintech.p2p.model.Investment;
import com.fintech.p2p.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {
    private final InvestmentService investmentService;

    // 投资人投资贷款
    @PostMapping("/invest")
    public ResponseEntity<Investment> invest(
            @RequestBody InvestmentRequest request) {
        Investment investment = investmentService.invest(
                request.getInvestorId(),
                request.getLoanId(),
                request.getAmount());
        return ResponseEntity.ok(investment);
    }

    // 查询投资记录（按投资人）
    @GetMapping("/list/investor/{investorId}")
    public ResponseEntity<List<Investment>> getInvestmentsByInvestor(
            @PathVariable(name = "investorId") Long investorId) {
        List<Investment> investments = investmentService.getInvestmentsByInvestor(investorId);
        return ResponseEntity.ok(investments);
    }

    // 查询投资记录（按贷款）
    @GetMapping("/list/loan/{loanId}")
    public ResponseEntity<List<Investment>> getInvestmentsByLoan(
            @PathVariable(name = "loanId") Long loanId) {
        List<Investment> investments = investmentService.getInvestmentsByLoan(loanId);
        return ResponseEntity.ok(investments);
    }

    // 确认投资
    @PostMapping("/confirm/{investmentId}")
    public ResponseEntity<Investment> confirmInvestment(
            @PathVariable(name = "investmentId") Long investmentId) {
        Optional<Investment> investmentOpt = investmentService.confirmInvestment(investmentId);
        return investmentOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 获取单个投资记录详情
     *
     * @param id 投资记录ID
     * @return 投资记录详情，如未找到则返回404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Investment> getInvestment(
            @PathVariable(name = "id") Long id) {
        return investmentService.findInvestmentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
