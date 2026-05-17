package com.ecommerce.user.mapper;

import com.ecommerce.user.model.User;
import com.ecommerce.user.DTOs.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {
    User toEntity(UserCreateDTO createDTO);

    UserResponseDTO toResponseDto(User entity);

    void updateEntityFromDto(UserUpdateDTO updateDto, @MappingTarget User entity);
}