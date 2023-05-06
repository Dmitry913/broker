package org.wasend.broker.service.interfaces;

import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

/**
 * Сервис для выполнения операций над сообщениями
 */
public interface MessageService {

    void addMessage(MessageModel producerMessage);

    void registry(RegistryModel consumerMessage);

    /**
     * Сколько всего мастер сообщений - на данному узле
     */
    Integer getCountMessage();
}
