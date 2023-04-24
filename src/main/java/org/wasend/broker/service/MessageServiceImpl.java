package org.wasend.broker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.wasend.broker.dao.QueueRepository;
import org.wasend.broker.dto.ConsumerMessage;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.service.mapper.DefaultMapper;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final QueueRepository repository;
    private final DefaultMapper<MessageModel, ProducerMessage> mapper;
    private final MapperFactory mapperFactory;

    @Override
    // todo тут нужно всем репликам отправить копию
    public void addMessage(ProducerMessage producerMessage) {
        repository.addMessage(mapper.mapTo(producerMessage));
    }

    @Override

    public void registry(ConsumerMessage consumerMessage) {
        repository.registry(mapperFactory.map(consumerMessage));
    }
}
