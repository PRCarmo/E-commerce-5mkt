package com.ecommerce.projetobackend.user;

import com.ecommerce.projetobackend.shared.exception.EmailAlreadyExistsException;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateMe(User authenticatedUser, UserUpdateMeRequest request) {
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", authenticatedUser.getId()));

        if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        userRepository.save(user);
        userRepository.flush();
        entityManager.refresh(user);
        return user;
    }

    public Page<User> listAll(User authenticatedUser, UserRole roleFilter, Pageable pageable) {
        if (authenticatedUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Access restricted to administrators");
        }
        if (roleFilter != null) {
            return userRepository.findByRole(roleFilter, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public User findByIdAsAdmin(Long id, User authenticatedUser) {
        if (authenticatedUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Access restricted to administrators");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }
}
