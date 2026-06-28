package com.trading.auth.service;

import com.trading.auth.dto.LoginRequest;
import com.trading.auth.dto.RegisterRequest;
import com.trading.auth.entity.RefreshToken;
import com.trading.auth.entity.User;
import com.trading.auth.repository.RefreshTokenRepository;
import com.trading.auth.repository.UserRepository;
import com.trading.auth.security.JwtTokenProvider;
import com.trading.shared.exception.TradingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .id(userId)
                .email("test@trading.dev")
                .passwordHash("$2a$12$hashed")
                .fullName("Test User")
                .role("ROLE_USER")
                .kycStatus("VERIFIED")
                .active(true)
                .build();
    }

    // ─── Register Tests ──────────────────────────────────────────

    @Test
    @DisplayName("register: should create user and return tokens when valid request")
    void register_ShouldCreateUserAndReturnTokens() {
        var request = new RegisterRequest();
        request.setEmail("new@trading.dev");
        request.setPassword("Password@1");
        request.setFullName("New User");

        when(userRepository.existsByEmail("new@trading.dev")).thenReturn(false);
        when(passwordEncoder.encode("Password@1")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        var response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register: should throw 409 when email already exists")
    void register_ShouldThrow409WhenEmailExists() {
        var request = new RegisterRequest();
        request.setEmail("existing@trading.dev");
        request.setPassword("Password@1");
        request.setFullName("Existing User");

        when(userRepository.existsByEmail("existing@trading.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(TradingException.class)
                .satisfies(ex -> {
                    var tradingEx = (TradingException) ex;
                    assertThat(tradingEx.getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
                    assertThat(tradingEx.getHttpStatus()).isEqualTo(409);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: mock KYC should always be VERIFIED")
    void register_ShouldSetKycStatusToVerified() {
        var request = new RegisterRequest();
        request.setEmail("kyc@trading.dev");
        request.setPassword("Password@1");
        request.setFullName("KYC User");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User savedUser = User.builder().id(UUID.randomUUID()).email("kyc@trading.dev")
                .passwordHash("hashed").fullName("KYC User").kycStatus("VERIFIED")
                .role("ROLE_USER").active(true).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("tok");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("ref");
        when(jwtTokenProvider.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        var response = authService.register(request);

        assertThat(response.getUser().getKycStatus()).isEqualTo("VERIFIED");
    }

    // ─── Login Tests ─────────────────────────────────────────────

    @Test
    @DisplayName("login: should return tokens when credentials are valid")
    void login_ShouldReturnTokens_WhenCredentialsValid() {
        var request = new LoginRequest();
        request.setEmail("test@trading.dev");
        request.setPassword("Password@1");

        when(userRepository.findByEmail("test@trading.dev")).thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());

        var response = authService.login(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("test@trading.dev");
        verify(refreshTokenRepository).revokeAllByUser(mockUser);
    }

    @Test
    @DisplayName("login: should propagate BadCredentialsException when credentials invalid")
    void login_ShouldThrow_WhenBadCredentials() {
        var request = new LoginRequest();
        request.setEmail("test@trading.dev");
        request.setPassword("WrongPassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ─── Logout Tests ────────────────────────────────────────────

    @Test
    @DisplayName("logout: should revoke all refresh tokens for user")
    void logout_ShouldRevokeAllRefreshTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        authService.logout(userId);

        verify(refreshTokenRepository).revokeAllByUser(mockUser);
    }

    // ─── Get Profile Tests ────────────────────────────────────────

    @Test
    @DisplayName("getProfile: should return user DTO")
    void getProfile_ShouldReturnUserDto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        var dto = authService.getProfile(userId);

        assertThat(dto.getId()).isEqualTo(userId);
        assertThat(dto.getEmail()).isEqualTo("test@trading.dev");
        assertThat(dto.getKycStatus()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("getProfile: should throw 404 when user not found")
    void getProfile_ShouldThrow404WhenUserNotFound() {
        var unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile(unknownId))
                .isInstanceOf(TradingException.class)
                .satisfies(ex -> assertThat(((TradingException) ex).getHttpStatus()).isEqualTo(404));
    }
}
