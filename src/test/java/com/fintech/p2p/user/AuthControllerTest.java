package com.fintech.p2p.user;

import com.fintech.p2p.dto.LoginRequest;
import com.fintech.p2p.dto.RegisterRequest;
import com.fintech.p2p.dto.UserDto;
import com.fintech.p2p.model.User;
import com.fintech.p2p.security.JwtTokenUtil;
import com.fintech.p2p.service.UserService;
import com.fintech.p2p.controller.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    @Mock
    private UserService userService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private User user;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegisterSuccess() {
        when(userService.register(any(UserDto.class))).thenReturn(user);

        ResponseEntity<?> response = authController.register(registerRequest);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(user, response.getBody());
    }

    @Test
    void testRegisterFailure() {
        when(userService.register(any(UserDto.class))).thenThrow(new RuntimeException("Registration failed"));

        Exception exception = assertThrows(RuntimeException.class, () -> authController.register(registerRequest));
        assertEquals("Registration failed", exception.getMessage());
    }

    @Test
    void testLoginSuccess() {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(jwtTokenUtil.generateToken("testuser")).thenReturn("fake-jwt-token");
        when(jwtTokenUtil.getTokenExpirationTime(anyString())).thenReturn(3600L);

        ResponseEntity<?> response = authController.login(loginRequest);
        assertEquals(200, response.getStatusCodeValue());

        // 使用 assertInstanceOf 直接检查类型并转换
        Map<String, Object> responseMap = assertInstanceOf(Map.class, response.getBody());

        assertEquals("fake-jwt-token", responseMap.get("token"));
        assertEquals(3600L, responseMap.get("tokenExpiresIn"));
        assertEquals("testuser", responseMap.get("username"));
    }

    @Test
    void testLoginFailure_WrongUsername() {
        when(userService.findByUsername("wronguser")).thenReturn(Optional.empty());

        LoginRequest wrongRequest = new LoginRequest();
        wrongRequest.setUsername("wronguser");
        wrongRequest.setPassword("wrongpass");

        ResponseEntity<?> response = authController.login(wrongRequest);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid credentials", response.getBody());
    }

    @Test
    void testLoginFailure_WrongPassword() {
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", user.getPassword())).thenReturn(false);

        LoginRequest wrongRequest = new LoginRequest();
        wrongRequest.setUsername("testuser");
        wrongRequest.setPassword("wrongpass");

        ResponseEntity<?> response = authController.login(wrongRequest);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals("Invalid credentials", response.getBody());
    }
}
