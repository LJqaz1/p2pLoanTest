package com.fintech.p2p.service;

import com.fintech.p2p.config.EmailProperties;
import com.fintech.p2p.exception.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件服务类，负责发送各类邮件通知
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 发送逾期还款通知邮件
     *
     * @param toEmail 收件人邮箱
     * @param loanId  贷款ID
     * @param amount  逾期金额
     * @return 异步任务的CompletableFuture
     */
    @Async("emailTaskExecutor")
    @Retryable(value = {MessagingException.class, EmailSendingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Boolean> sendOverdueNotification(String toEmail, String loanId, BigDecimal amount) {
        validateEmail(toEmail);

        log.info("准备发送逾期还款通知邮件到 {}", toEmail);
        String subject = "【重要】您的贷款还款已逾期，请尽快处理！";

        // 准备模板数据
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("loanId", loanId);
        templateModel.put("amount", amount);
        templateModel.put("repaymentUrl", emailProperties.getRepaymentUrl());

        return sendHtmlEmail(toEmail, subject, "email/overdue-notification", templateModel);
    }

    /**
     * 发送贷款批准通知邮件
     *
     * @param toEmail 收件人邮箱
     * @param loanId  贷款ID
     * @param amount  贷款金额
     * @param dueDate 到期日
     * @return 异步任务的CompletableFuture
     */
    @Async("emailTaskExecutor")
    @Retryable(value = {MessagingException.class, EmailSendingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Boolean> sendLoanApprovedNotification(
            String toEmail, Long loanId, BigDecimal amount, LocalDate dueDate) {
        validateEmail(toEmail);

        log.info("准备发送贷款批准通知邮件到 {}", toEmail);
        String subject = "【恭喜】您的贷款申请已获批准！";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("loanId", loanId);
        templateModel.put("amount", amount);
        templateModel.put("dueDate", dueDate);
        templateModel.put("dashboardUrl", emailProperties.getDashboardUrl());

        return sendHtmlEmail(toEmail, subject, "email/loan-approved", templateModel);
    }

    /**
     * 发送还款成功通知邮件
     *
     * @param toEmail         收件人邮箱
     * @param loanId          贷款ID
     * @param amount          还款金额
     * @param remainingAmount 剩余本金
     * @return 异步任务的CompletableFuture
     */
    @Async("emailTaskExecutor")
    @Retryable(value = {MessagingException.class, EmailSendingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Boolean> sendRepaymentSuccessNotification(
            String toEmail, Long loanId, BigDecimal amount, BigDecimal remainingAmount) {
        validateEmail(toEmail);

        log.info("准备发送还款成功通知邮件到 {}", toEmail);
        String subject = "【通知】您的还款已成功处理";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("loanId", loanId);
        templateModel.put("amount", amount);
        templateModel.put("remainingAmount", remainingAmount);
        templateModel.put("dashboardUrl", emailProperties.getDashboardUrl());

        return sendHtmlEmail(toEmail, subject, "email/repayment-success", templateModel);
    }

    /**
     * 使用Thymeleaf模板引擎处理邮件模板
     *
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return 处理后的HTML内容
     */
    private String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    /**
     * 验证邮箱地址
     *
     * @param email 要验证的邮箱地址
     * @throws IllegalArgumentException 如果邮箱地址为空或格式无效
     */
    private void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            log.error("邮箱地址为空，无法发送通知");
            throw new IllegalArgumentException("邮箱地址不能为空");
        }

        // 使用更健壮的邮箱验证正则表达式
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(regex)) {
            log.error("邮箱地址格式无效: {}", email);
            throw new IllegalArgumentException("邮箱地址格式无效: " + email);
        }
    }

    /**
     * 发送电子邮件的通用方法
     *
     * @param toEmail       收件人邮箱
     * @param subject       邮件主题
     * @param templateName  模板名称
     * @param templateModel 模板数据
     * @return 异步发送结果
     */
    private CompletableFuture<Boolean> sendHtmlEmail(
            String toEmail, String subject, String templateName, Map<String, Object> templateModel) {

        String emailContent = processTemplate(templateName, templateModel);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailProperties.getSenderEmail(), emailProperties.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("📩 邮件 '{}' 已成功发送到 {}", subject, toEmail);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("❌ 发送邮件 '{}' 到 {} 失败：{}", subject, toEmail, e.getMessage());
            throw new EmailSendingException("发送邮件失败", e);
        }
    }

    public CompletableFuture<Boolean> sendEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailProperties.getSenderEmail(), emailProperties.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("📩 邮件 '{}' 已成功发送到 {}", subject, toEmail);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("❌ 发送邮件 '{}' 到 {} 失败：{}", subject, toEmail, e.getMessage());
            throw new EmailSendingException("发送邮件失败", e);
        }
    }
}