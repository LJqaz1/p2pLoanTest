package com.fintech.p2p.controller;

import com.fintech.p2p.dto.LoginRequest;
import com.fintech.p2p.dto.RegisterRequest;
import com.fintech.p2p.dto.UserDto;
import com.fintech.p2p.model.User;
import com.fintech.p2p.security.JwtTokenUtil;
import com.fintech.p2p.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    // 登录尝试计数器 - 在生产环境中应该使用Redis等分布式缓存
    private final Map<String, AtomicInteger> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockoutTimes = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    // 构造函数注入，避免 `@Autowired` 可能导致 NPE
    public AuthController(UserService userService, JwtTokenUtil jwtTokenUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Received registration request for username: {}", registerRequest.getUsername());
        // 检查用户名是否已存在
        if (userService.findByUsername(registerRequest.getUsername()).isPresent()) {
            logger.info("Registration failed: Username '{}' already exists", registerRequest.getUsername());
            return ResponseEntity.badRequest().body("Username already exists");
        }

        // 验证密码强度
        if (!isPasswordStrong(registerRequest.getPassword())) {
            logger.warn("Registration failed: Weak password for username '{}'", registerRequest.getUsername());
            return ResponseEntity.badRequest().body(
                    "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character");
        }

        // 创建新用户
        UserDto userDto = new UserDto();
        userDto.setUsername(registerRequest.getUsername());
        userDto.setEmail(registerRequest.getEmail());
        userDto.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        try {
            User registeredUser = userService.register(userDto);
            logger.info("User registered successfully: {}", userDto.getUsername());

            // 返回用户信息（不包含密码）
            Map<String, Object> response = new HashMap<>();
            response.put("id", registeredUser.getId());
            response.put("username", registeredUser.getUsername());
            response.put("message", "Registration successful");

            // 添加安全头
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");

            return ResponseEntity.ok().headers(headers).body(response);
        } catch (Exception e) {
            logger.error("Error occurred while registering user '{}': {}", registerRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();

        // 检查账户是否被锁定
        if (isAccountLocked(username)) {
            logger.warn("Login attempt for locked account: {}", username);
            return ResponseEntity.status(429).body("Too many failed attempts. Account temporarily locked.");
        }

        Optional<User> existingUser = userService.findByUsername(username);

        // 验证登录凭证
        if (existingUser.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), existingUser.get().getPassword())) {
            // 记录失败尝试
            recordFailedAttempt(username);
            logger.warn("Failed login attempt for user: {}", username);
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // 登录成功，重置尝试计数器
        resetLoginAttempts(username);
        User user = existingUser.orElseThrow(() -> new RuntimeException("User not found"));

        // 生成JWT令牌
        String token = jwtTokenUtil.generateToken(existingUser.get().getUsername());
        String refreshToken = generateRefreshToken(user);
        logger.info("User logged in successfully: {}", username);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        response.put("username", existingUser.get().getUsername());
        response.put("tokenExpiresIn", jwtTokenUtil.getTokenExpirationTime(token));

        // 添加安全头
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("Cache-Control", "no-store");
        headers.add("Pragma", "no-cache");

        return ResponseEntity.ok().headers(headers).body(response);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || !userService.isValidRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        Optional<User> user = userService.findUserByRefreshToken(refreshToken);
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        String newToken = jwtTokenUtil.generateToken(user.get().getUsername());
        String newRefreshToken = generateRefreshToken(user.get());

        Map<String, Object> response = new HashMap<>();
        response.put("token", newToken);
        response.put("refreshToken", newRefreshToken);
        response.put("tokenExpiresIn", jwtTokenUtil.getTokenExpirationTime(newToken));

        return ResponseEntity.ok(response);
    }

    /**
     * 申请密码重置
     */
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        String username = request.getUsername();
        Optional<User> user = userService.findByUsername(username);

        if (user.isPresent()) {
            // 生成唯一重置令牌并设置过期时间（24小时）
            String resetToken = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

            userService.savePasswordResetToken(user.get().getId(), resetToken, expiryDate);
            logger.info("Password reset token generated for user: {}", username);

            // 在生产环境中，这里应该发送邮件
            // emailService.sendResetEmail(user.get().getEmail(), resetToken);
        }

        // 防止枚举攻击，不管用户是否存在都返回相同信息
        return ResponseEntity.ok("If your account exists, you will receive password reset instructions.");
    }

    /**
     * 验证密码重置令牌
     */
    @GetMapping("/reset-password/verify")
    public ResponseEntity<?> verifyResetToken(@RequestParam("token") String token) {
        if (!userService.isValidPasswordResetToken(token)) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        return ResponseEntity.ok("Token is valid");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        if (!userService.isValidPasswordResetToken(token)) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        // 验证密码强度
        if (!isPasswordStrong(newPassword)) {
            return ResponseEntity.badRequest().body(
                    "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character");
        }

        Optional<User> user = userService.findUserByResetToken(token);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        // 更新密码
        userService.updatePassword(user.get().getId(), passwordEncoder.encode(newPassword));

        // 使重置令牌失效
        userService.deleteByPasswordResetToken(token);

        logger.info("Password reset successful for user: {}", user.get().getUsername());

        return ResponseEntity.ok("Password has been reset successfully");
    }

    /**
     * 注销
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtil.getUsernameFromToken(token);

        // 在实际应用中，将令牌加入黑名单
        // jwtTokenUtil.blacklistToken(token);

        logger.info("User logged out: {}", username);

        return ResponseEntity.ok("Logged out successfully");
    }

    // ====== 辅助方法 ======

    private boolean isPasswordStrong(String password) {
        // 至少8个字符，包含大小写字母、数字和特殊字符
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(regex);
    }

    private boolean isAccountLocked(String username) {
        LocalDateTime lockoutTime = lockoutTimes.get(username);
        if (lockoutTime != null) {
            if (LocalDateTime.now().isBefore(lockoutTime)) {
                return true;
            } else {
                // 锁定时间已过
                lockoutTimes.remove(username);
                loginAttempts.remove(username);
            }
        }
        return false;
    }

    private void recordFailedAttempt(String username) {
        AtomicInteger attempts = loginAttempts.computeIfAbsent(username, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        if (currentAttempts >= MAX_ATTEMPTS) {
            // 锁定账户
            lockoutTimes.put(username, LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            logger.warn("Account locked due to too many failed attempts: {}", username);
        }
    }

    private void resetLoginAttempts(String username) {
        loginAttempts.remove(username);
        lockoutTimes.remove(username);
    }

    private String generateRefreshToken(User user) {
        String refreshToken = UUID.randomUUID().toString();
        // 设置30天过期
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
        userService.updateRefreshToken(user.getId(), refreshToken, expiryDate);
        return refreshToken;
    }
}

@Setter
@Getter
class ResetPasswordRequest {
    @NotBlank(message = "Username is required")
    private String username;
}

@Setter
@Getter
class NewPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}