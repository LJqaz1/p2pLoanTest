package com.fintech.p2p.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app.email")
@Data
@Validated
public class EmailProperties {

    @NotBlank(message = "发件人邮箱不能为空")
    @Email(message = "发件人邮箱格式不正确")
    private String senderEmail;

    @NotBlank(message = "发件人名称不能为空")
    private String senderName = "P2P借贷平台";

    @NotBlank(message = "还款URL不能为空")
    private String repaymentUrl = "https://your-p2p-platform.com/repayment";

    @NotBlank(message = "控制面板URL不能为空")
    private String dashboardUrl = "https://your-p2p-platform.com/dashboard";
}