package com.ecommerce.projetobackend.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.projetobackend.security.JwtService;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setup() throws Exception {

        jwtService = new JwtService();

        Field secretField =
                JwtService.class.getDeclaredField("secret");

        secretField.setAccessible(true);

        secretField.set(
                jwtService,
                "mySuperSecretKeyForJwtTests123456789012345"
        );

        Field expirationField =
                JwtService.class.getDeclaredField(
                        "expirationMs"
                );

        expirationField.setAccessible(true);

        expirationField.set(
                jwtService,
                3600000L
        );

        var initMethod =
                JwtService.class.getDeclaredMethod(
                        "init"
                );

        initMethod.setAccessible(true);

        initMethod.invoke(jwtService);
    }

    @Test
    void mustGenerateValidToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setRole(UserRole.CUSTOMER);

        String token =
                jwtService.generateToken(user);

        assertNotNull(token);

        assertFalse(token.isBlank());
    }

    @Test
    void mustExtractEmailFromToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setRole(UserRole.CUSTOMER);

        String token =
                jwtService.generateToken(user);

        String email =
                jwtService.extractEmail(token);

        assertEquals(
                "test@email.com",
                email
        );
    }

    @Test
    void mustValidateCorrectToken() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setRole(UserRole.CUSTOMER);

        String token =
                jwtService.generateToken(user);

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        "test@email.com"
                );

        assertTrue(valid);
    }

    @Test
    void mustRejectTokenForDifferentEmail() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setRole(UserRole.CUSTOMER);

        String token =
                jwtService.generateToken(user);

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        "other@email.com"
                );

        assertFalse(valid);
    }

    @Test
    void mustRejectMalformedToken() {

        boolean valid =
                jwtService.isTokenValid(
                        "this-is-not-a-jwt",
                        "test@email.com"
                );

        assertFalse(valid);
    }
}
