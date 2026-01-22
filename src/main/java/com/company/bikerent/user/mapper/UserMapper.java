package com.company.bikerent.user.mapper;

import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.dto.UserDto;
import com.company.bikerent.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    
    UserDto toDto(User user);
    
    UserResponse toResponse(User user);
    
    User toEntity(UserDto dto);
}
