package org.wasend.broker.dao.impl;

import org.wasend.broker.dao.interfaces.DataStructure;
import org.wasend.broker.service.model.MessageModel;

import java.util.Comparator;
import java.util.PriorityQueue;

public class DataStructureProducerMessage implements DataStructure<MessageModel> {
    // PriorityQueue - временная сложность операций enqueuing и dequeuing O(log(n))
    // todo можно было бы сделать свою структуру данных, где операция вставки занимала бы O(lon(n)) - бинарный поиск, а операция извлечения O(1) - при использовании массива постоянного объёма
    private final PriorityQueue<MessageModel> queue;

    public DataStructureProducerMessage() {
        queue = new PriorityQueue<>(Comparator.comparing(MessageModel::getDeadLine));
    }

    @Override
    public void add(MessageModel elem) {
        queue.add(elem);
    }

    @Override
    public MessageModel getMin() {
        return queue.poll();
    }
}
