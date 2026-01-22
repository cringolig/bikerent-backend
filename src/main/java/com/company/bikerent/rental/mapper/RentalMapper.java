package com.company.bikerent.rental.mapper;

import com.company.bikerent.rental.domain.Rental;
import com.company.bikerent.rental.dto.RentalDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RentalMapper {
    
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "bicycle.id", target = "bicycleId")
    @Mapping(source = "startStation.id", target = "startStationId")
    @Mapping(source = "endStation.id", target = "endStationId")
    @Mapping(source = "rentalStartedAt", target = "startedAt")
    @Mapping(source = "rentalEndedAt", target = "endedAt")
    @Mapping(expression = "java(rental.getStatus().name())", target = "status")
    RentalDto toDto(Rental rental);
}
