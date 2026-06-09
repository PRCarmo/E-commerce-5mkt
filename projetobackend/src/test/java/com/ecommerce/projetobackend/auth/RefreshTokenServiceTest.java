package com.ecommerce.projetobackend.auth;

import com.ecommerce.projetobackend.shared.exception.InvalidRefreshTokenException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
    }

    @Test
    void createTokenSavesOnlyHashAndReturnsRawToken() {
        User user = customer();

        String rawToken = refreshTokenService.createToken(user);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        assertEquals(user, savedToken.getUser());
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertEquals(hash(rawToken), savedToken.getTokenHash());
        assertFalse(savedToken.isRevoked());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void rotateRevokesOldRefreshTokenAndCreatesNewOne() {
        User user = customer();
        String oldRawToken = "old-refresh-token";
        RefreshToken oldToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(oldRawToken))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(hash(oldRawToken))).thenReturn(Optional.of(oldToken));

        RefreshTokenService.RefreshTokenRotation rotation = refreshTokenService.rotate(oldRawToken);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());

        RefreshToken newToken = tokenCaptor.getAllValues().get(1);
        assertTrue(oldToken.isRevoked());
        assertEquals(user, rotation.user());
        assertNotNull(rotation.refreshToken());
        assertNotEquals(oldRawToken, rotation.refreshToken());
        assertEquals(hash(rotation.refreshToken()), newToken.getTokenHash());
        assertFalse(newToken.isRevoked());
    }

    @Test
    void rotateRejectsUnknownRefreshToken() {
        String rawToken = "unknown-refresh-token";
        when(refreshTokenRepository.findByTokenHash(hash(rawToken))).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));
        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rotateRejectsExpiredRefreshToken() {
        String rawToken = "expired-refresh-token";
        RefreshToken expiredToken = RefreshToken.builder()
                .user(customer())
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken))).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));
        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rotateRejectsRevokedRefreshToken() {
        String rawToken = "revoked-refresh-token";
        RefreshToken revokedToken = RefreshToken.builder()
                .user(customer())
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(hash(rawToken))).thenReturn(Optional.of(revokedToken));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));
        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private User customer() {
        return User.builder()
                .id(1L)
                .name("Customer")
                .email("customer@mkt5.com")
                .password("encoded-password")
                .role(UserRole.CUSTOMER)
                .build();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
