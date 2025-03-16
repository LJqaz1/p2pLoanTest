package com.fintech.p2p.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.p2p.model.Investment;
import com.fintech.p2p.repository.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService {
    private final InvestmentRepository investmentRepository;

    // 投资人投资贷款
    public Investment invest(Long investorId, Long loanId, BigDecimal amount) {
        Investment investment = new Investment();
        investment.setInvestorId(investorId);
        investment.setLoanId(loanId);
        investment.setAmount(amount);
        investment.setStatus("PENDING"); // 默认 PENDING 状态
        return investmentRepository.save(investment);
    }

    // 查询投资记录（按投资人）
    public List<Investment> getInvestmentsByInvestor(Long investorId) {
        return investmentRepository.findByInvestorId(investorId);
    }

    // 查询投资记录（按贷款）
    public List<Investment> getInvestmentsByLoan(Long loanId) {
        return investmentRepository.findByLoanId(loanId);
    }

    // 确认投资
    public Optional<Investment> confirmInvestment(Long investmentId) {
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        investmentOpt.ifPresent(investment -> {
            investment.setStatus("CONFIRMED");
            investmentRepository.save(investment);
        });
        return investmentOpt;
    }

    public FileChannel getInvestmentById(Long id) {
        try {
            // 查找投资记录
            Optional<Investment> investment = investmentRepository.findById(id);

            if (investment.isEmpty()) {
                log.warn("Investment with ID {} not found", id);
                return null;
            }

            // 创建临时文件
            Path tempFile = Files.createTempFile("investment_" + id + "_", ".json");

            // 将投资记录转换为JSON并写入文件
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(tempFile.toFile(), investment.get());

            log.info("Created temporary file for investment ID {}: {}", id, tempFile);

            // 打开并返回文件通道
            return FileChannel.open(
                    tempFile,
                    StandardOpenOption.READ,
                    StandardOpenOption.DELETE_ON_CLOSE // 确保文件在通道关闭后被删除
            );

        } catch (IOException e) {
            log.error("Error creating file for investment ID: {}", id, e);
            throw new RuntimeException("Failed to generate investment file: " + e.getMessage(), e);
        }
    }

    /**
     * 根据ID获取投资记录
     *
     * @param id 投资记录ID
     * @return 投资记录，如果不存在则返回空Optional
     */
    public Optional<Investment> findInvestmentById(Long id) {
        return investmentRepository.findById(id);
    }
}