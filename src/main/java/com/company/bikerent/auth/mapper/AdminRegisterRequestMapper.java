package com.company.bikerent.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.company.bikerent.auth.domain.AdminRegisterRequest;
import com.company.bikerent.auth.dto.AdminRegisterRequestDto;
import com.company.bikerent.auth.dto.AdminRegisterRequestRequest;
import com.company.bikerent.user.mapper.UserMapper;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {UserMapper.class})
public interface AdminRegisterRequestMapper {

  @Mapping(source = "id", target = "requestId")
  AdminRegisterRequestDto toDto(AdminRegisterRequest entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "updatedDate", ignore = true)
  AdminRegisterRequest toEntity(AdminRegisterRequestRequest request);
}
