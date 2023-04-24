package org.wasend.broker.dao;

import org.springframework.stereotype.Component;
import org.wasend.broker.dto.ProducerMessage;

import java.util.ArrayDeque;
import java.util.Queue;

public class DataStructureProducerMessage implements DataStructure<ProducerMessage> {
    // todo очередь с поддержкой порядка? - минимума??. Где порядок сортировки - это время отправки сообщения
    private static final Queue<ProducerMessage> queue = new ArrayDeque<>();

    @Override
    public void add(ProducerMessage elem) {
        queue.add(elem);
    }

    @Override
    public ProducerMessage getMin() {
        return queue.poll();
    }
}
