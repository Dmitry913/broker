package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.dto.ConsumerMessage;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.model.SyncMessage;
import org.wasend.broker.service.mapper.DefaultMapper;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSender messageSender;
    private final QueueRepository repository;
    private final DefaultMapper<MessageModel, ProducerMessage> mapper;
    private final MapperFactory mapperFactory;

    @Override
    // todo тут нужно всем репликам отправить копию
    public void addMessage(ProducerMessage producerMessage) {
        repository.addMessage(mapper.mapTo(producerMessage));
    }

    @Override
    // todo всем репликам отправить копию (тут наверное лучше сделать синхронный механизм по умолчанию
    public void registry(ConsumerMessage consumerMessage) {
        // сохраняем сообщение в наше хранилище
        repository.registry(mapperFactory.mapTo(consumerMessage, RegistryModel.class));
        // отправляем сообщение для синхронизации другим брокерам
        messageSender.syncMessage(mapperFactory.mapTo(consumerMessage, SyncMessage.class));
    }
}
