package com.ecommerce.projetobackend.services;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.auth.AuthResponse;
import com.ecommerce.projetobackend.auth.AuthService;
import com.ecommerce.projetobackend.auth.RegisterRequest;
import com.ecommerce.projetobackend.security.JwtService;
import com.ecommerce.projetobackend.shared.exception.EmailAlreadyExistsException;
import com.ecommerce.projetobackend.user.UserRole;
import com.ecommerce.projetobackend.user.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    
    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void mustThrowExceptionWhenRegisterAdmin() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Teste");
        request.setEmail("teste@email.com");
        request.setPassword("12345678");
        request.setRole(UserRole.ADMIN);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.register(request)
                );

        assertEquals(
                "Cannot register as ADMIN",
                exception.getMessage()
        );
    }

    @Test
    void mustThrowExceptionWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Teste");
        request.setEmail("teste@email.com");
        request.setPassword("12345678");
        request.setRole(UserRole.CUSTOMER);

        when(userService.existsByEmail("teste@email.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );
    }

    @Test
    void mustRegisterUserSucessfully() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Test");
        request.setEmail("test@email.com");
        request.setPassword("12345678");
        request.setRole(UserRole.CUSTOMER);

        User user = User.builder()
                .name("Test")
                .email("test@email.com")
                .password("encodedPassword")
                .role(UserRole.CUSTOMER)
                .build();

        when(userService.existsByEmail("test@email.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("12345678"))
                .thenReturn("encodedPassword");

        when(userService.findByEmail("test@email.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        AuthResponse response =
                authService.register(request);

        assertNotNull(response);

        assertEquals(
                "jwt-token",
                response.getToken()
        );

        verify(userService)
                .save(any(User.class));
    }

    @Test
    void mustThrowExceptionWhenUserNotFoundAfterSaving() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Test");
        request.setEmail("test@email.com");
        request.setPassword("12345678");
        request.setRole(UserRole.CUSTOMER);

        when(userService.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("password");

        when(userService.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> authService.register(request)
        );
    }
}
