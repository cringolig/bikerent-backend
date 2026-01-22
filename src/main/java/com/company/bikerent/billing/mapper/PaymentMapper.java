package com.company.bikerent.billing.mapper;

import com.company.bikerent.billing.domain.Payment;
import com.company.bikerent.billing.dto.PaymentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {
    
    @Mapping(source = "user.id", target = "userId")
    PaymentDto toDto(Payment payment);
}
