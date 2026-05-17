package com.ecommerce.user.DTOs;

import lombok.Data;
import com.ecommerce.enums.UserRole;

@Data
public class UserResponseDTO {

    private String name;
    private String email;
    private String password;
    private UserRole role;
    
}