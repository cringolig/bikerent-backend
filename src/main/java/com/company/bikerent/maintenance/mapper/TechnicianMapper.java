package com.company.bikerent.maintenance.mapper;

import com.company.bikerent.maintenance.domain.Technician;
import com.company.bikerent.maintenance.dto.CreateTechnicianRequest;
import com.company.bikerent.maintenance.dto.TechnicianDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TechnicianMapper {
    
    TechnicianDto toDto(Technician entity);
    
    @Mapping(target = "id", ignore = true)
    Technician toEntity(CreateTechnicianRequest request);
}
