package com.company.bikerent.billing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.company.bikerent.billing.domain.Payment;
import com.company.bikerent.billing.dto.PaymentDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

  @Mapping(source = "user.id", target = "userId")
  PaymentDto toDto(Payment payment);
}
