package com.fintech.p2p.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationService {
    private final EmailService emailService;

    @Autowired
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * 发送还款成功通知
     *
     * @param toEmail            借款人邮箱
     * @param loanId             贷款ID
     * @param amount             还款金额
     * @param remainingPrincipal 剩余本金
     * @return 发送结果 CompletableFuture
     */
    public CompletableFuture<Boolean> sendRepaymentNotification(String toEmail, Long loanId, BigDecimal amount, BigDecimal remainingPrincipal) {
        log.info("准备发送还款成功通知至 {}", toEmail);

        String subject = "【P2Pレンディングプラットフォーム】ご返済完了のお知らせ";
        String content = String.format(
                "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #e3e3e3; border-radius: 5px;'>" +
                        "<h2 style='color: #4CAF50;'>ご入金完了のお知らせ</h2>" +
                        "<p>お客様各位</p>" +
                        "<p>平素は格別のご高配を賜り、誠にありがとうございます。</p>" +
                        "<p>このたび、お客様のご返済が正常に処理されましたことをお知らせいたします。</p>" +
                        "<p><strong>ご返済金額:</strong> ¥%s</p>" +
                        "<p><strong>残りの元金:</strong> ¥%s</p>" +
                        "<p>期日通りのご返済、誠にありがとうございます。これはお客様の良好な信用記録の維持に貢献いたします。</p>" +
                        "<p>ご不明な点がございましたら、いつでもカスタマーサービスチームまでお問い合わせください。</p>" +
                        "<p>今後とも変わらぬお引き立てを賜りますよう、よろしくお願い申し上げます。</p>" +
                        "<p>敬具<br>P2Pレンディングプラットフォーム</p>" +
                        "</div>",
                amount.setScale(2, RoundingMode.HALF_UP),
                remainingPrincipal.setScale(2, RoundingMode.HALF_UP)
        );

        return emailService.sendEmail(toEmail, subject, content);
    }

    /**
     * 监听 Kafka 逾期还款消息，并发送通知
     */
    @KafkaListener(topics = "overdue-repayment-topic", groupId = "notification-group")
    public void sendOverdueNotification(String message) {
        log.info("!!!逾期还款通知：{}", message);

        // 集成邮件 发送提醒（Gmail SMTP）
        try {
            // 假设 message 格式: "借款人邮箱,贷款ID,金额"
            String[] parts = message.split(",");
            if (parts.length != 3) {
                log.error("消息格式错误: {}", message);
                return;
            }
            String email = parts[0];
            String loanId = parts[1];
            BigDecimal amount = new BigDecimal(parts[2]);

            // 发送邮件通知
            CompletableFuture<Boolean> future = emailService.sendOverdueNotification(
                    email,
                    loanId,
                    amount
            );

            future.thenAccept(success -> {
                if (success) {
                    log.info("已成功发送逾期还款通知至 {}", email);
                } else {
                    log.warn("逾期还款通知发送失败，接收方: {}", email);
                }
            });

        } catch (Exception e) {
            log.error("处理逾期还款通知时出错: {}", e.getMessage(), e);
        }
    }
}