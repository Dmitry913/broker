package org.wasend.broker.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.wasend.broker.dto.ConsumerMessageRegistry;
import org.wasend.broker.service.model.RegistryModel;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public class ConsumerRegistryToRegistryModelMapper implements DefaultMapper<RegistryModel, ConsumerMessageRegistry> {
    @Override
    public RegistryModel mapTo(ConsumerMessageRegistry e) {
        return RegistryModel.builder()
                .timeRegistration(LocalDateTime.now())
                .url(e.getHost() + ":" + e.getPort())
                .build();
    }
}
