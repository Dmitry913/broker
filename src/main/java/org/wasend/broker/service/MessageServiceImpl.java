package org.wasend.broker.service;

import org.springframework.stereotype.Service;
import org.wasend.broker.dto.ProducerMessage;

import java.util.ArrayDeque;
import java.util.Queue;

@Service
public class MessageServiceImpl implements MessageService {
    @Override
    public void addMessage(ProducerMessage producerMessage) {
//        queue.add(producerMessage);
    }
}
