package org.wasend.broker.service.interfaces;

import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.SyncMessage;

/**
 * Сервис для рассылки сообщений внешним системам (других брокерам и consumer-ам).
 * Данный класс должен отравлять сообщения consumer-ам, при наступлении deadline.
 * Так же он должен отправлять сообщения другим брокерам, для обеспечения механизма репликации.
 */
public interface MessageSender {

    /**
     * метод постоянно отправляет сообщения, по мере того, как наступает дедлайн
     */
    void startSending();

    /**
     * Метод для синхронизации сообщений между очередями
     */
    void sendSynchronizationMessage(SyncMessage message, String topicName);

    /**
     * Метод для синхронизации адресов всех зарегистрировавшихся consumer-ов.
     */
    // todo Адрес консьюмеров должна определить и прислать клиентская библиотека
    void sendSynchronizationMessage(SyncMessage message);

    /**
     * Пересылаем сообщение на другой узел в ходе балансировки нагрузки
     */
    // todo реализовать
    void delegateMessage(MessageModel producerMessage, String addressNode);

}
