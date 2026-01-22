package com.company.bikerent.bicycle.mapper;

import com.company.bikerent.bicycle.domain.Bicycle;
import com.company.bikerent.bicycle.domain.BicycleStatus;
import com.company.bikerent.bicycle.domain.BicycleType;
import com.company.bikerent.bicycle.dto.BicycleDto;
import com.company.bikerent.bicycle.dto.CreateBicycleRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BicycleMapper {
    
    @Mapping(source = "station.id", target = "stationId")
    @Mapping(source = "type", target = "type", qualifiedByName = "typeToString")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    BicycleDto toDto(Bicycle entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "station", ignore = true)
    @Mapping(target = "lastServiceDate", ignore = true)
    @Mapping(target = "mileage", ignore = true)
    @Mapping(target = "repairs", ignore = true)
    @Mapping(source = "type", target = "type", qualifiedByName = "stringToType")
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToStatusWithDefault")
    Bicycle toEntity(CreateBicycleRequest request);
    
    @Named("typeToString")
    default String typeToString(BicycleType type) {
        return type != null ? type.name() : null;
    }
    
    @Named("statusToString")
    default String statusToString(BicycleStatus status) {
        return status != null ? status.name() : null;
    }
    
    @Named("stringToType")
    default BicycleType stringToType(String type) {
        return type != null ? BicycleType.valueOf(type) : null;
    }
    
    @Named("stringToStatusWithDefault")
    default BicycleStatus stringToStatusWithDefault(String status) {
        if (status == null || status.isBlank()) {
            return BicycleStatus.AVAILABLE;
        }
        return BicycleStatus.valueOf(status);
    }
}