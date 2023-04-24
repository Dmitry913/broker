package org.wasend.broker.dao;

import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

/**
 * Данный класс должен работать с файлами, извлекать из них информации и хранить её
 */
public interface QueueRepository {
    /**
     * Добавляет сообщение в очередь и в файловую систему
     */
    void addMessage(MessageModel model);

    /**
     * Отдаёт сообщение, дедлайн которого наступил.
     * Данный механизм обеспечивается тем, что наверху очереди всегда сообщение, дедлайн которого минимальный.
     */
    MessageModel getMessage();

    void registry(RegistryModel model);
}
