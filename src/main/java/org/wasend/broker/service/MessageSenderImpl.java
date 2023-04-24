package org.wasend.broker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.wasend.broker.dao.QueueRepository;
import org.wasend.broker.dto.ProducerMessage;

@Service
@RequiredArgsConstructor
public class MessageSenderImpl implements MessageSender {

    private final QueueRepository queueRepository;

    // скорее всего должно быть в ассинхроне
    @Override
    public void startSend() {
        // данная функция не должна ответить, пока не наступит deadline сообщения
        ProducerMessage producerMessage = queueRepository.getMessage();

    }
}
