package com.ecommerce.projetobackend.services;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.Collections;

import com.ecommerce.projetobackend.auth.AuthService;
import com.ecommerce.projetobackend.shared.exception.EmailAlreadyExistsException;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.user.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    
    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void mustRejectUpdateWhenEmailAlreadyExists() {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);

        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setName("Test");
        existingUser.setEmail("test@email.com");

        UserUpdateMeRequest request = new UserUpdateMeRequest();
        request.setName("NewTest");
        request.setEmail("new@email.com");

        when(userRepository.findById(2L))
            .thenReturn(Optional.of(existingUser));

        when(userRepository.existsByEmail("new@email.com"))
            .thenReturn(true);

        EmailAlreadyExistsException exception =
            assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.updateMe(authenticatedUser, request)
            );

        assertEquals(
            "This email already exists", 
            exception.getMessage()
        );
        
        verify(userRepository)
            .findById(2L);

        verify(userRepository)
            .findByEmail("new@email.com");

        verify(userRepository, never())
            .save(any(User.class));
    }

    @Test
    void mustUpdateOwnProfileSuccessfully() {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);

        User user = new User();
        user.setId(2L);
        user.setName("Test");
        user.setEmail("test@email.com");

        UserUpdateMeRequest request = new UserUpdateMeRequest();
        request.setName("NewTest");
        request.setEmail("new@email.com");

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByEmail("new@email.com"))
                .thenReturn(false);

        User result =
                userService.updateMe(
                        authenticatedUser,
                        request
                );

        assertNotNull(result);

        assertEquals(
                "newTest",
                result.getName()
        );

        assertEquals(
                "new@email.com",
                result.getEmail()
        );

        verify(userRepository)
                .findById(2L);

        verify(userRepository)
                .existsByEmail("new@email.com");

        verify(userRepository)
                .save(user);

        verify(userRepository)
                .flush();

        verify(entityManager)
                .refresh(user);
    }
    
    @Test
    void mustDenyListAllForNonAdmin() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        Pageable pageable = PageRequest.of(0, 10);

        ForbiddenException exception =
            assertThrows(
                ForbiddenException.class,
                () -> userService.listAll(user, null, pageable)
        );

        assertEquals(
            "Access restricted to administrators", 
            exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }
    
    @Test
    void mustAllowAdminToListAllUsers() {

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        Pageable pageable =
                PageRequest.of(0, 10);

        Page<User> page =
                new PageImpl<>(Collections.emptyList());

        when(userRepository.findAll(pageable))
                .thenReturn(page);

        Page<User> result =
                userService.listAll(
                        admin,
                        null,
                        pageable
                );

        assertNotNull(result);

        verify(userRepository)
                .findAll(pageable);
    }
    
    @Test
    void mustForbidFindUserByIdForNonAdmin() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        ForbiddenException exception =
            assertThrows(
                ForbiddenException.class,
                () -> userService.findByIdAsAdmin(1L, user)
        );

        assertEquals(
            "Access restricted to administrators", 
            exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }
    
    @Test
    void mustAllowAdminToFindUserById() {

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("User Test");

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(targetUser));

        User result =
                userService.findByIdAsAdmin(
                        2L,
                        admin
                );

        assertNotNull(result);

        assertEquals(
                2L,
                result.getId()
        );

        assertEquals(
                "User Test",
                result.getName()
        );

        verify(userRepository)
                .findById(2L);
    }
    
    @Test
    void mustThrowEntityNotFoundWhenUpdatingNonExistingUser() {

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);

        UserUpdateMeRequest request =
                new UserUpdateMeRequest();

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.updateMe(
                        authenticatedUser,
                        request
                )
        );

        verify(userRepository)
                .findById(1L);

        verify(userRepository, never())
                .save(any(User.class));

        verify(userRepository, never())
                .flush();

        verify(entityManager, never())
                .refresh(any());
    }

}
