package com.ecommerce.user.controller;

import com.ecommerce.user.DTOs.*;
import com.ecommerce.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO post(
        
        @RequestBody UserCreateDTO dto

    ) {
        
        return service.post(dto);
    }

    @GetMapping
    public List<UserResponseDTO> get() {
        
        return service.get();
    }

    @GetMapping("/{id}")
    public UserResponseDTO getById(
        @PathVariable Long id
    ) {

        return service.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponseDTO put(
        
        @PathVariable Long id,
        @RequestBody UserUpdateDTO dto
    ) {

        return service.put(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        
        @PathVariable Long id
    ) {

        service.delete(id);
    }
}
