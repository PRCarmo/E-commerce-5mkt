package com.ecommerce.projetobackend.controllers;

import com.ecommerce.projetobackend.auth.*;
import com.ecommerce.projetobackend.config.SecurityConfig;
import com.ecommerce.projetobackend.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void mustRejectRegisterWhenEmailIsInvalid() throws Exception {
        
        RegisterRequest request = new RegisterRequest();
        request.setName("Pedro");
        request.setEmail("email-invalido");
        request.setPassword("12345678");
        request.setRole(UserRole.CUSTOMER);

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void mustRegisterUserSuccessfully() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@email.com");
        request.setPassword("12345678");
        request.setRole(UserRole.CUSTOMER);

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token")
                .value("jwt-token"));

        verifyNoInteractions(authService);
    }

    @Test
    void mustRejectLoginWhenEmailIsInvalid() throws Exception {
        
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("12345678");

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void mustRejectLoginWhenPasswordIsBlank() throws Exception {
        
        LoginRequest request = new LoginRequest();
        request.setEmail("test@email.com");
        request.setPassword("");

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void mustLoginUserSucessfully() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@email.com");
        request.setPassword("12345678");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token")
                .value("jwt-token"));
    }
}