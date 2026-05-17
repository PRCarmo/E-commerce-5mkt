package com.ecommerce.user.DTOs;

import lombok.Data;
import com.ecommerce.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class UserCreateDTO {

    @NotBlank
    @Size(max = 30)
    private String name;

    @NotBlank
    private String email;

    @NotBlank
    @Size(max = 30, min = 6)
    private String password;

    @NotNull
    private UserRole role;
}