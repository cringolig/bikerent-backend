package com.company.bikerent.station.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.company.bikerent.geo.domain.Coordinates;
import com.company.bikerent.geo.dto.CoordinatesDto;
import com.company.bikerent.station.domain.Station;
import com.company.bikerent.station.dto.CreateStationRequest;
import com.company.bikerent.station.dto.StationDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StationMapper {

  StationDto toDto(Station entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "availableBicycles", ignore = true)
  @Mapping(target = "bicycles", ignore = true)
  Station toEntity(CreateStationRequest request);

  CoordinatesDto toDto(Coordinates coordinates);

  Coordinates toEntity(CoordinatesDto dto);
}
