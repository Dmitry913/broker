package org.wasend.broker.dao.impl;

import org.springframework.stereotype.Repository;
import org.wasend.broker.dao.interfaces.DataStructure;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Repository
public class QueueRepositoryImpl implements QueueRepository {

    // todo перенести хранение в файл
    private final DataStructure<MessageModel> dataStructure;
    // todo перенести хранение в файл
    private final Set<String> urlConsumers;

    public QueueRepositoryImpl() {
        dataStructure = new DataStructureProducerMessage();
        urlConsumers = new HashSet<>();
    }

    // у сообщений должны быть какие-то id
    @Override
    public void addMessage(MessageModel model) {
        // todo вставить сообщение в правильное место в очереди
        dataStructure.add(model);
    }

    // todo нужно обработать кейс, когда тут висит в ожидание сообщение, которое ждёт своего времени обработки,
    //  а нам приходит новое сообщение, у которого дедлайн раньше текущего.
    @Override
    public MessageModel getMessage() {
        MessageModel message = dataStructure.getMin();
//        wait();
        while (message.getDeadLine().isBefore(LocalDateTime.now())) {

        }
        return dataStructure.getMin();
    }

    @Override
    public void registry(RegistryModel model) {

    }
}
