package com.trading.auth.service;

import com.trading.auth.dto.*;
import com.trading.auth.entity.RefreshToken;
import com.trading.auth.entity.User;
import com.trading.auth.repository.RefreshTokenRepository;
import com.trading.auth.repository.UserRepository;
import com.trading.auth.security.JwtTokenProvider;
import com.trading.shared.exception.ResourceNotFoundException;
import com.trading.shared.exception.TradingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user. KYC is auto-verified (mock).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new TradingException("Email already registered", "EMAIL_ALREADY_EXISTS", 409);
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new TradingException("Phone already registered", "PHONE_ALREADY_EXISTS", 409);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .kycStatus("VERIFIED")    // mock — always verified
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} (id={})", user.getEmail(), user.getId());

        return buildAuthResponse(user);
    }

    /**
     * Authenticate user and issue tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException if invalid — Spring Security handles it
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        // Revoke all previous refresh tokens for clean state
        refreshTokenRepository.revokeAllByUser(user);

        return buildAuthResponse(user);
    }

    /**
     * Rotate refresh token — revoke old, issue new access + refresh tokens.
     */
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new TradingException("Invalid or expired refresh token",
                        "INVALID_REFRESH_TOKEN", 401));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new TradingException("Refresh token expired", "REFRESH_TOKEN_EXPIRED", 401);
        }

        // Revoke old token (rotation)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return buildAuthResponse(user);
    }

    /**
     * Logout — revoke all refresh tokens for the user.
     */
    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        refreshTokenRepository.revokeAllByUser(user);
        log.info("User logged out: {}", userId);
    }

    /**
     * Get user profile by ID.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return toDto(user);
    }

    // ─── Private helpers ──────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store hashed refresh token
        RefreshToken storedToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpiryMs()))
                .build();
        refreshTokenRepository.save(storedToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)          // 15 minutes in seconds
                .user(toDto(user))
                .build();
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .kycStatus(user.getKycStatus())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
