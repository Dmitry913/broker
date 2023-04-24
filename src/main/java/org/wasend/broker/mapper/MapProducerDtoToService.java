package org.wasend.broker.mapper;

import org.mapstruct.Mapper;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.model.MessageModel;

@Mapper(componentModel = "spring")
public interface MapProducerDtoToService extends DefaultMapper<MessageModel, ProducerMessage> {
}