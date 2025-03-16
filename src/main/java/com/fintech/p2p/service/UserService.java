package com.fintech.p2p.service;

import com.fintech.p2p.dto.UserDto;
import com.fintech.p2p.model.User;
import com.fintech.p2p.repository.UserRepository;
import org.slf4j.ILoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole("BORROWER");

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void updateRefreshToken(Long id, String refreshToken, LocalDateTime expiryDate) {
        if (id == null || refreshToken == null || expiryDate == null) {
            throw new IllegalArgumentException("User ID, refresh token, and expiry date must not be null");
        }

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User with ID " + id + " does not exist");
        }

        userRepository.updateRefreshToken(id, refreshToken, expiryDate);
    }

    public boolean isValidRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken).isPresent();
    }

    public Optional<User> findUserByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }

    @Transactional
    public void savePasswordResetToken(Long id, String resetToken, LocalDateTime expiryDate) {
        userRepository.updatePasswordResetToken(id, resetToken, expiryDate);
    }

    public boolean isValidPasswordResetToken(String token) {
        return userRepository.findByPasswordResetToken(token).isPresent();
    }

    public Optional<User> findUserByResetToken(String token) {
        return userRepository.findByPasswordResetToken(token);
    }

    @Transactional
    public void updatePassword(Long id, String newPassword) {
        userRepository.updatePassword(id, passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void deleteByPasswordResetToken(String token) {
        userRepository.deleteByPasswordResetToken(token);
    }
}