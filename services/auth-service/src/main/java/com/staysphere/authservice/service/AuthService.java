package com.staysphere.authservice.service;
import com.staysphere.authservice.model.*;
import com.staysphere.authservice.repository.*;
import com.staysphere.shared.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public AuthResult register(String email, String password, String firstName,
            String lastName, String phone, List<String> roles) {
        if (userRepository.existsByEmail(email.toLowerCase()))
            throw new IllegalArgumentException("An account with this email already exists");
        String verificationToken = UUID.randomUUID().toString();
        User user = User.builder()
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName).lastName(lastName).phone(phone)
                .roles(new HashSet<>(roles != null ? roles : List.of("GUEST")))
                .status(User.AccountStatus.PENDING_VERIFICATION)
                .emailVerificationToken(verificationToken)
                .build();
        User saved = userRepository.save(user);
        kafkaTemplate.send("auth.user.registered",
                Map.of("userId", saved.getId(), "email", saved.getEmail(),
                       "firstName", firstName, "verificationToken", verificationToken));
        return new AuthResult(null, null, saved.getId(), email, saved.getRoles(), false);
    }

    @Transactional
    public AuthResult login(String email, String password, String ipAddress, String deviceInfo) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now()))
            throw new RuntimeException("Account temporarily locked. Try again later.");
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5)
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            throw new RuntimeException("Invalid credentials");
        }
        if (user.getStatus() == User.AccountStatus.SUSPENDED)
            throw new RuntimeException("Your account has been suspended");
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
        List<String> roles = new ArrayList<>(user.getRoles());
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roles);
        String refreshToken = generateAndSaveRefreshToken(user.getId(), ipAddress, deviceInfo);
        return new AuthResult(accessToken, refreshToken, user.getId(),
                user.getEmail(), user.getRoles(), user.getTwoFactorEnabled());
    }

    @Transactional
    public AuthResult refreshTokens(String refreshTokenValue) {
        String hash = hashToken(refreshTokenValue);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (rt.getRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            throw new RuntimeException("Refresh token expired or revoked");
        }
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<String> roles = new ArrayList<>(user.getRoles());
        String newAccess = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roles);
        String newRefresh = generateAndSaveRefreshToken(user.getId(), rt.getIpAddress(), rt.getDeviceInfo());
        return new AuthResult(newAccess, newRefresh, user.getId(), user.getEmail(), user.getRoles(), false);
    }

    @Transactional
    public void logout(String userId, String refreshTokenValue) {
        if (refreshTokenValue != null) {
            String hash = hashToken(refreshTokenValue);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
        redisTemplate.opsForValue().set("blacklist:user:" + userId, "1", Duration.ofDays(7));
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiry(LocalDateTime.now().plusHours(2));
            userRepository.save(user);
            kafkaTemplate.send("auth.password.reset.requested",
                    Map.of("userId", user.getId(), "email", user.getEmail(), "resetToken", token));
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        if (user.getPasswordResetExpiry().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Reset token has expired");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        refreshTokenRepository.deleteByUserId(user.getId());
        userRepository.save(user);
    }

    @Transactional
    public User updateProfile(String userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (updates.containsKey("firstName")) user.setFirstName(updates.get("firstName"));
        if (updates.containsKey("lastName")) user.setLastName(updates.get("lastName"));
        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        if (updates.containsKey("profileImageUrl")) user.setProfileImageUrl(updates.get("profileImageUrl"));
        return userRepository.save(user);
    }

    private String generateAndSaveRefreshToken(String userId, String ip, String deviceInfo) {
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId).tokenHash(hashToken(token))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .ipAddress(ip).deviceInfo(deviceInfo).build());
        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException("Hash error", e); }
    }

    public record AuthResult(String accessToken, String refreshToken, String userId,
                              String email, Set<String> roles, Boolean twoFactorRequired) {}
}
