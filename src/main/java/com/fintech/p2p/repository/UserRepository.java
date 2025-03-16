package com.fintech.p2p.repository;

import com.fintech.p2p.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> getUserByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :refreshToken, u.tokenExpiry = :expiry WHERE u.id = :id")
    void updateRefreshToken(@Param("id") Long id,
                            @Param("refreshToken") String refreshToken,
                            @Param("expiry") LocalDateTime expiryDate);

    Optional<User> findByRefreshToken(String refreshToken);

    @Modifying
    @Query("UPDATE User u SET u.password = :hashedPassword WHERE u.id = :id")
    void updatePassword(@Param("id") Long id,
                        @Param("hashedPassword") String hashedPassword);

    @Modifying
    @Query("UPDATE User u SET u.passwordResetToken = :resetToken, u.passwordResetExpiry = :expiryDate WHERE u.id = :id")
    void updatePasswordResetToken(@Param("id") Long id,
                                  @Param("resetToken") String resetToken,
                                  @Param("expiryDate") LocalDateTime expiryDate);

    Optional<User> findByPasswordResetToken(String token);

    default void deleteByPasswordResetToken(String token) {
        findByPasswordResetToken(token).ifPresent(this::delete);
    }

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
