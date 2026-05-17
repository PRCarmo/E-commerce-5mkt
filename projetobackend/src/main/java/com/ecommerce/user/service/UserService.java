package com.ecommerce.user.service;

import com.ecommerce.user.DTOs.*;
import com.ecommerce.user.model.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
// import RuntimeException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    public UserResponseDTO post(UserCreateDTO dto) {
        User user = mapper.toEntity(dto);

        User saved = repository.save(user);

        return mapper.toResponseDto(saved);
    }

    public List<UserResponseDTO> get() {
        
        return repository.findAll()
            .stream()
            .map(mapper::toResponseDto)
            .toList();
    }

    public UserResponseDTO getById(
        Long id
    ) {
        User user = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("User not found")
        );

        return mapper.toResponseDto(user);
    }

    public UserResponseDTO put(
        Long id, UserUpdateDTO dto
    ) {
        
        User user = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("User not found")
        );

        mapper.updateEntityFromDto(dto, user);
        User updated = repository.save(user);
        
        return mapper.toResponseDto(updated);
    }

    public void delete(
        Long id
    ) {
        User user = repository.findById(id)
            .orElseThrow(() ->
            new RuntimeException("User not found")
        );

        repository.delete(user);
    }
}