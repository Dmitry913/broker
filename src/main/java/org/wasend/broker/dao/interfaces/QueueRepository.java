package org.wasend.broker.dao.interfaces;

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
     * Отдаёт сообщение, дедлайн которого наступил. Отвечает за контроль времени отправки сообщения.
     * Данный механизм обеспечивается тем, что наверху очереди всегда сообщение, дедлайн которого минимальный.
     * Данная функция по своей сути является publisher-ом в push модели Reactor Framework
     */
    MessageModel getMessage();

    /**
     * Зарегистрировать нового consumer-а
     */
    boolean registry(RegistryModel model);

    /**
     * Возвращает кол-во сообщений, для которых является master-node, найденные по названию топика
     */
    Integer getMessagesCount(String topicName);
    /**
     * Возвращает кол-во сообщений, для которых является master-node
     * @return
     */
    Integer getAllMessageCount();

    void movePartitionToProcessing(String partitionId);
}
