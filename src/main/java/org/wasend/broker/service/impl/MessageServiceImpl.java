package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.model.SyncMessage;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSender messageSender;
    private final QueueRepository repository;
    private final MapperFactory mapperFactory;

    @Override
    public void addMessage(MessageModel producerMessage) {

        // сохраняем сообщение в наше хранилище
        repository.addMessage(producerMessage);
        // отправляем сообщение для синхронизации другим брокерам
        messageSender.syncMessage(mapperFactory.mapTo(producerMessage, SyncMessage.class), producerMessage.getTopicName());
    }

    @Override
    // todo всем репликам отправить копию (тут наверное лучше сделать синхронный механизм по умолчанию
    public void registry(RegistryModel consumerMessage) {
        // сохраняем сообщение в наше хранилище
        repository.registry(consumerMessage);
        // отправляем сообщение для синхронизации другим брокерам
        messageSender.syncMessage(mapperFactory.mapTo(consumerMessage, SyncMessage.class));
    }
}
