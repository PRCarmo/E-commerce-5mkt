package com.ecommerce.projetobackend.user;

import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userService.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("User", userDetails.getUser().getId()));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UserUpdateMeRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User updated = userService.updateMe(userDetails.getUser(), request);
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listAll(
            @RequestParam(required = false) UserRole role,
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Page<UserResponse> page = userService.listAll(userDetails.getUser(), role, pageable)
                .map(UserResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userService.findByIdAsAdmin(id, userDetails.getUser());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
