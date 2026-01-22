package com.company.bikerent.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.company.bikerent.user.domain.User;
import com.company.bikerent.user.dto.UserDto;
import com.company.bikerent.user.dto.UserResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  UserDto toDto(User user);

  UserResponse toResponse(User user);

  User toEntity(UserDto dto);
}
