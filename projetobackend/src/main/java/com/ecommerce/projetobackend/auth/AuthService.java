package com.ecommerce.projetobackend.auth;

import com.ecommerce.projetobackend.security.JwtService;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import com.ecommerce.projetobackend.shared.exception.EmailAlreadyExistsException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserResponse;
import com.ecommerce.projetobackend.user.UserRole;
import com.ecommerce.projetobackend.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserService userService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot register as ADMIN");
        }

        if (userService.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userService.save(user);

        // re-fetch para popular createdAt (campo insertable=false, JPA não recarrega após save)
        User saved = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after save: " + request.getEmail()));

        String token = jwtService.generateToken(saved);
        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(saved))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .build();
    }
}
