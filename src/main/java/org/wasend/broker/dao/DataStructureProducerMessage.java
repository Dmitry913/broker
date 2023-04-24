package org.wasend.broker.dao;

import org.wasend.broker.service.model.MessageModel;

import java.util.ArrayDeque;
import java.util.Queue;

public class DataStructureProducerMessage implements DataStructure<MessageModel> {
    // todo очередь с поддержкой порядка? - минимума??. Где порядок сортировки - это время отправки сообщения
    private static final Queue<MessageModel> queue = new ArrayDeque<>();

    @Override
    public void add(MessageModel elem) {
        queue.add(elem);
    }

    @Override
    public MessageModel getMin() {
        return queue.poll();
    }
}
