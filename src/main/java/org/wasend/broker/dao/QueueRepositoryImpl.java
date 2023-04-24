package org.wasend.broker.dao;

import org.springframework.stereotype.Repository;
import org.wasend.broker.dto.ProducerMessage;
import org.wasend.broker.model.MessageModel;

import java.time.LocalDateTime;


@Repository
public class QueueRepositoryImpl implements QueueRepository {

    private final DataStructure<MessageModel> dataStructure;

    public QueueRepositoryImpl() {
        dataStructure = new DataStructureProducerMessage();
    }

    // у сообщений должны быть какие-то id
    @Override
    public void addMessage(MessageModel producerMessage) {
        // todo вставить сообщение в правильное место в очереди
        dataStructure.add(producerMessage);
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
}
