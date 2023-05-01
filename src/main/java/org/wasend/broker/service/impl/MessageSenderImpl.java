package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.wasend.broker.dao.interfaces.MetaInfoRepository;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.exception.SendingMessageException;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.SyncMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSenderImpl implements MessageSender {

    private final WebClient webClient;
    private final QueueRepository queueRepository;
    private final MetaInfoRepository metaInfoRepository;

    // todo скорее всего должно быть в ассинхроне
    // todo можно было бы запустить в несколько потоков данное действие
    //  (но у них у всех должен быть один Repository, и если один поток забрал сообщение, то другой уже не должен брать его)
    @Override
    public void startSending() {
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
                        // todo можно было бы в хедеры прикладывать ip-адрес системы
                        //  (внутри шифта?? тогда получается надо ip-adress ноды прикладывать, то есть сервера)
                        .header("service", "server-id")
                        .exchangeToMono(clientResponse -> {
                            if (!clientResponse.statusCode().equals(HttpStatus.OK)) {
                                log.error("Error while sending request to {}", url);
                                return Mono.error(new SendingMessageException(message.getId(), url));
                            }
                            return clientResponse.bodyToMono(String.class);
                        });
            }
        }
    }

    @Override
    public void syncMessage(SyncMessage message) {
        Flux.just(metaInfoRepository.getAllNodesAddress().toArray(new String[]{}))
                .flatMap(address -> {
                            message.setId(UUID.randomUUID().toString());
                            return webClient.post()
                                    .uri(address)
                                    .bodyValue(message)
                                    .header("service", "server-id")
                                    .exchangeToMono(clientResponse -> {
                                        if (!clientResponse.statusCode().equals(HttpStatus.OK)) {
                                            log.error("Error while sending synchronization registry to {}", address);
                                            return Mono.error(new SendingMessageException(message.getId(), address));
                                        }
                                        return clientResponse.bodyToMono(String.class);
                                    });
                        }
                ).subscribe();
    }

    @Override
    public void syncMessage(SyncMessage message, String topicName) {
        // todo возможно тут можно как-то сделать через flux (отправка всем хостам из sendTo)
        for (String address : metaInfoRepository.getReplicasAddress(topicName)) {
            // т.к. все методы возвращают новые объекты не будет проблем при параллельной работе с методов startSend
            webClient.post()
                    .uri(address)
                    .bodyValue(message)
                    // todo можно было бы в хедеры прикладывать ip-адрес системы
                    //  (внутри шифта?? тогда получается надо ip-adress ноды прикладывать, то есть сервера)
                    .header("service", "server-id")
                    // todo определить политику retry??
                    .exchangeToMono(clientResponse -> {
                        if (!clientResponse.statusCode().equals(HttpStatus.OK)) {
                            log.error("Error while sending synchronization message to {}", address);
                            return Mono.error(new SendingMessageException(message.getId(), address));
                        }
                        return clientResponse.bodyToMono(String.class);
                    });
        }
    }
}
