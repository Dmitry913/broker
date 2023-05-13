package org.wasend.broker.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.wasend.broker.dto.BrokerMessage;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.service.model.MessageModel;

@Mapper(componentModel = "spring")
public interface MapMessageModelToBrokerMessage extends DefaultMapper<BrokerMessage, MessageModel>{

    @Override
    @Mapping(source = "e", target = "producerMessage")
    BrokerMessage mapTo(MessageModel e);

    ProducerMessage mapMessage(MessageModel e);
}
