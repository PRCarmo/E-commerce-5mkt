package com.ecommerce.user.DTOs;

import lombok.Data;
import com.ecommerce.enums.UserRole;
import jakarta.validation.constraints.Size;

@Data
public class UserUpdateDTO {

    @Size(max = 30)
    private String name;
    
    private String email;

    @Size(max = 30, min = 6)
    private String password;

    private UserRole role;
}