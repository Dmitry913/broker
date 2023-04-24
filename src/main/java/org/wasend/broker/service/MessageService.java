package org.wasend.broker.service;

import org.wasend.broker.dto.ProducerMessage;

public interface MessageService {

    void addMessage(ProducerMessage producerMessage);
}
