package org.wasend.broker.service.interfaces;

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
    void syncMessage(SyncMessage message, String topicName);

    /**
     * Метод для синхронизации адресов всех зарегистрировавшихся consumer-ов.
     */
    // todo Адрес консьюмеров должна определить и прислать клиентская библиотека
    void syncMessage(SyncMessage message);

}
