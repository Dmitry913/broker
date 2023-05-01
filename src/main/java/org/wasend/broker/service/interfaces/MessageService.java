package org.wasend.broker.service.interfaces;

import org.wasend.broker.dto.ConsumerMessage;
import org.wasend.broker.dto.ProducerMessage;

/**
 * Сервис для обработки сообщений от producer и consumer
 */
public interface MessageService {

    void addMessage(ProducerMessage producerMessage);

    void registry(ConsumerMessage consumerMessage);
}
