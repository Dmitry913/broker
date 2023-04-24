package org.wasend.broker.service;

/**
 * Данный класс должен отравлять сообщения consumer-ам, при наступлении deadline.
 * Так же он должен отправлять сообщения другим брокерам, для обеспечения механизма репликации.
 */
public interface MessageSender {

    void startSend();

}
