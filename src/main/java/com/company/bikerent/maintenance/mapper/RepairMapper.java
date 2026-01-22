package com.company.bikerent.maintenance.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.company.bikerent.maintenance.domain.Repair;
import com.company.bikerent.maintenance.dto.RepairDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RepairMapper {

  @Mapping(source = "bicycle.id", target = "bicycleId")
  @Mapping(source = "technician.id", target = "technicianId")
  @Mapping(source = "repairStartedAt", target = "startedAt")
  @Mapping(source = "repairEndedAt", target = "endedAt")
  @Mapping(expression = "java(repair.getStatus().name())", target = "status")
  RepairDto toDto(Repair repair);
}
