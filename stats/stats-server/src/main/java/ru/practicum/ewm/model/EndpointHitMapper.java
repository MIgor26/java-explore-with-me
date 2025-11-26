package ru.practicum.ewm.model;

import org.mapstruct.Mapper;
import ru.practicum.ewm.EndpointHitDto;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {

   EndpointHit toHit(EndpointHitDto endpointHitDto);

   EndpointHitDto toHitDto(EndpointHit endpointHit);
}