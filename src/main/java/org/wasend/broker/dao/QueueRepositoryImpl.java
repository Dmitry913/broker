package org.wasend.broker.dao;

import org.springframework.stereotype.Repository;
import org.wasend.broker.dto.ProducerMessage;

import java.time.LocalDateTime;


@Repository
public class QueueRepositoryImpl implements QueueRepository {

    private final DataStructure<ProducerMessage> dataStructure;

    public QueueRepositoryImpl() {
        dataStructure = new DataStructureProducerMessage();
    }

    @Override
    public void addMessage(ProducerMessage producerMessage) {
        // todo вставить сообщение в правильное место в очереди
        dataStructure.add(producerMessage);
    }

    // todo нужно обработать кейс, когда тут висит в ожидание сообщение, которое ждёт своего времени обработки,
    //  а нам приходит новое сообщение, у которого дедлайн раньше текущего.
    @Override
    public ProducerMessage getMessage() {
        ProducerMessage message = dataStructure.getMin();
//        wait();
        while (message.getDeadLine().isBefore(LocalDateTime.now())) {

        }
        return dataStructure.getMin();
    }
}
