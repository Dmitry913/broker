package org.wasend.broker.service.mapper;

import org.mapstruct.Mapper;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.service.model.Message;
import org.wasend.broker.service.model.MessageModel;

@Mapper(componentModel = "spring")
public interface MapProducerDtoToService extends DefaultMapper<MessageModel, ProducerMessage> {

    @Override
    MessageModel mapTo(ProducerMessage e);
}