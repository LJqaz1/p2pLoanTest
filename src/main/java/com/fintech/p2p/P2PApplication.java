package com.fintech.p2p;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@SpringBootApplication  // 确保 Spring Boot 会扫描到配置类
public class P2PApplication {
    public static void main(String[] args) {
        SpringApplication.run(P2PApplication.class, args);
    }
}
