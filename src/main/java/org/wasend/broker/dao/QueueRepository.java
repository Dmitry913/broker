package org.wasend.broker.dao;

import org.wasend.broker.model.MessageModel;

/**
 * Данный класс должен работать с файлами, извлекать из них информации и хранить её
 */
public interface QueueRepository {
    /**
     * Добавляет сообщение в очередь и в файловую систему
     */
    void addMessage(MessageModel producerMessage);

    /**
     * Отдаёт сообщение, дедлайн которого наступил.
     * Данный механизм обеспечивается тем, что наверху очереди всегда сообщение, дедлайн которого минимальный.
     */
    MessageModel getMessage();
}
