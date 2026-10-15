package com.digital.smartteca.search.mapper;

import org.mapstruct.Mapper;
import java.util.UUID;

/**
 * Documentos do Elasticsearch usam id como String (é assim que o
 * DomainEvent chega do Kafka); as Responses da API usam UUID, igual
 * ao core-service. Esses helpers fazem essa conversao pros outros
 * mappers reaproveitarem.
 */
@Mapper(componentModel = "spring")
public interface ComumMapper {

    default String map(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    default UUID map(String value) {
        return value != null ? UUID.fromString(value) : null;
    }

}
