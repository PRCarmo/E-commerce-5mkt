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
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserService userService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
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

        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.RefreshTokenRotation rotation = refreshTokenService.rotate(request.getRefreshToken());
        User user = rotation.user();

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .refreshToken(rotation.refreshToken())
                .user(UserResponse.from(user))
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .refreshToken(refreshTokenService.createToken(user))
                .user(UserResponse.from(user))
                .build();
    }
}
