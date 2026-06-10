package com.ecommerce.projetobackend.controllers;

import com.ecommerce.projetobackend.user.*;
import com.ecommerce.projetobackend.config.SecurityConfig;
import com.ecommerce.projetobackend.support.WebSecurityTestConfig;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, WebSecurityTestConfig.class})
public class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void mustGetOwnProfileSuccessfully() throws Exception {

        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setName("Test");
        userEntity.setEmail("test@email.com");
        userEntity.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(userEntity);

        when(userService.findById(1L))
                .thenReturn(java.util.Optional.of(userEntity));

        mockMvc.perform(
                get("/api/v1/users/me")
                        .with(user(userDetails))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test"))
        .andExpect(jsonPath("$.email")
                .value("test@email.com"))
        .andExpect(jsonPath("$.role")
                .value("CUSTOMER"));
    }

    @Test
    void mustUpdateOwnProfileSuccessfully() throws Exception {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setName("Test");
        authenticatedUser.setEmail("old@email.com");
        authenticatedUser.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(authenticatedUser);

        UserUpdateMeRequest request =
                new UserUpdateMeRequest();

        request.setName("NewTest");
        request.setEmail("new@email.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("NewTest");
        updatedUser.setEmail("new@email.com");
        updatedUser.setRole(UserRole.CUSTOMER);

        when(userService.updateMe(
                any(User.class),
                any(UserUpdateMeRequest.class)
        )).thenReturn(updatedUser);

        mockMvc.perform(
                put("/api/v1/users/me")
                        .with(user(userDetails))
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name")
                .value("NewTest"))
        .andExpect(jsonPath("$.email")
                .value("new@email.com"));
    }

    @Test
    void mustAllowAdminToListAllUsers() throws Exception {

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(admin);

        User user1 = new User();
        user1.setId(2L);
        user1.setName("Test");
        user1.setEmail("test@email.com");
        user1.setRole(UserRole.CUSTOMER);

        Page<User> page =
                new PageImpl<>(List.of(user1));

        when(userService.listAll(
                any(User.class),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                get("/api/v1/users")
                        .with(user(userDetails))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id")
                .value(2))
        .andExpect(jsonPath("$.content[0].name")
                .value("Test"));
    }

    @Test
    void mustAllowAdminToGetUserById() throws Exception {

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(admin);

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Test");
        targetUser.setEmail("test@email.com");
        targetUser.setRole(UserRole.CUSTOMER);

        when(userService.findByIdAsAdmin(
                2L,
                admin
        )).thenReturn(targetUser);

        mockMvc.perform(
                get("/api/v1/users/2")
                        .with(user(userDetails))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
                .value(2))
        .andExpect(jsonPath("$.name")
                .value("Test"));
    }

    @Test
    void mustRejectUpdateWhenNameIsBlank() throws Exception {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setRole(UserRole.CUSTOMER);
        UserDetailsImpl userDetails = new UserDetailsImpl(authenticatedUser);

        UserUpdateMeRequest request =
                new UserUpdateMeRequest();

        request.setName("");
        request.setEmail("test@email.com");

        mockMvc.perform(
                put("/api/v1/users/me")
                        .with(user(userDetails))
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void mustRejectUpdateWhenEmailIsInvalid() throws Exception {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setRole(UserRole.CUSTOMER);
        UserDetailsImpl userDetails = new UserDetailsImpl(authenticatedUser);

        UserUpdateMeRequest request =
                new UserUpdateMeRequest();

        request.setName("Test");
        request.setEmail("invalid-email");

        mockMvc.perform(
                put("/api/v1/users/me")
                        .with(user(userDetails))
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void mustReturn404WhenCurrentUserDoesNotExist() throws Exception {

        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(user);

        when(userService.findById(1L))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(
                get("/api/v1/users/me")
                        .with(user(userDetails))
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void mustReturn403WhenNonAdminListsUsers() throws Exception {

        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(userEntity);

        when(userService.listAll(
                any(),
                any(),
                any()
        ))
        .thenThrow(
                new ForbiddenException(
                        "Access restricted to administrators"
                )
        );

        mockMvc.perform(
                get("/api/v1/users")
                        .with(user(userDetails))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mustReturn403WhenNonAdminGetsUserById() throws Exception {

        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(userEntity);

        when(userService.findByIdAsAdmin(
                eq(2L),
                any(User.class)
        ))
        .thenThrow(
                new ForbiddenException(
                        "Access restricted to administrators"
                )
        );

        mockMvc.perform(
                get("/api/v1/users/2")
                        .with(user(userDetails))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mustReturn404WhenRequestedUserDoesNotExist() throws Exception {

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(admin);

        when(userService.findByIdAsAdmin(
                eq(999L),
                any(User.class)
        ))
        .thenThrow(
                new EntityNotFoundException(
                        "User",
                        999L
                )
        );

        mockMvc.perform(
                get("/api/v1/users/999")
                        .with(user(userDetails))
        )
        .andExpect(status().isNotFound());
    }
}
