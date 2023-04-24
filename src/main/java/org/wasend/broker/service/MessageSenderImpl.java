package org.wasend.broker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.wasend.broker.dao.QueueRepository;
import org.wasend.broker.exception.SendingCustomerException;
import org.wasend.broker.model.MessageModel;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSenderImpl implements MessageSender {

    private final WebClient webClient;
    private final QueueRepository queueRepository;

    // todo скорее всего должно быть в ассинхроне
    // todo можно было бы запустить в несколько потоков данное действие
    //  (но у них у всех должен быть один Repository, и если один поток забрал сообщение, то другой уже не должен брать его)
    @Override
    public void startSend() {
        while (true) {
            // todo данная функция не должна ответить, пока не наступит deadline сообщения
            MessageModel message = queueRepository.getMessage();
            // todo возможно тут можно как-то сделать через flux (отправка всем хостам из sendTo)
            for (String url : message.getSendTo()) {
                log.info("Send messageId={} to {}", message.getId(), url);
                // тут нужно сделать политику retry
                webClient
                        .post()
                        .uri(url)
                        .bodyValue(message.getPayload())
                        // todo можно было бы в хедеры прикладывать ip-адрес системы (внутри шифта?? тогда получается надо ip-adress ноды прикладывать, то есть сервера)
                        .header("service", "server-id")
                        .exchangeToMono(clientResponse -> {
                            if (!clientResponse.statusCode().equals(HttpStatus.OK)) {
                                log.error("Error while sending request to {}", url);
                                return Mono.error(new SendingCustomerException(message.getId(), url));
                            }
                            return clientResponse.bodyToMono(String.class);
                        });
            }
        }
    }
}
