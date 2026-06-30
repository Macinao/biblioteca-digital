package com.digital.biblioteca.mapper;

import org.mapstruct.Mapper;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ComumMapper {

    default String map(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    default UUID map(String value) {
        return value != null ? UUID.fromString(value) : null;
    }

}
