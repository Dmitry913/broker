package org.wasend.broker.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.wasend.broker.dto.BrokerMessage;
import org.wasend.broker.service.model.Message;
import org.wasend.broker.service.model.MessageModel;

@Mapper(componentModel = "spring")
public interface MapBrokerMessageToMessageModel extends DefaultMapper<MessageModel, BrokerMessage> {

    @Override
    @Mapping(target = "topicName", source = "e.producerMessage.topicName")
    @Mapping(target = "payload", source = "e.producerMessage.payload")
    @Mapping(target = "deadline", source = "e.producerMessage.deadline")
    @Mapping(target = "sendTo", source = "e.producerMessage.sendTo")
//    @Mapping(target = "ask", source = "e.producerMessage.ask")
    MessageModel mapTo(BrokerMessage e);
}
