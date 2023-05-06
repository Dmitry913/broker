package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.exception.SendingMessageException;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.SyncMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

import static org.wasend.broker.utils.HelpUtils.getWithProtocol;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSenderImpl implements MessageSender {

    private final WebClient webClient;
    private final QueueRepository queueRepository;
    private final ZooKeeperRepository zooKeeperRepository;
    @Value("replica.protocol")
    private final String replicaProtocol;
    private final static String PATH_FOR_SYNCHRONIZATION = "/v1/broker/updateReplica";

    // todo можно было бы запустить в несколько потоков данное действие
    //  (но у них у всех должен быть один Repository, и если один поток забрал сообщение, то другой уже не должен брать его)
    @Override
//    @Async - todo проверить, что запускается в отдельном потоке
    public void startSending() {
        Flux.<MessageModel>create(fluxSink -> {
            while (true) {
                // todo данная функция не должна ответить, пока не наступит deadline сообщения
                fluxSink.next(queueRepository.getMessage());
            }
        }).log().subscribe(message -> Flux.fromIterable(message.getSendTo())
                .log()
                // todo можно в несколько потоков сделать
                .flatMap(address ->
                        // todo тут нужно сделать политику retry - есть метод retryWhen
                        webClient.post()
                                .uri(address)
                                .bodyValue(message.getPayload())
                                // todo можно было бы в хедеры прикладывать ip-адрес системы
                                //  (внутри шифта?? тогда получается надо ip-adress ноды прикладывать, то есть сервера)
                                .header("service", "server-id")
                                .exchangeToMono(clientResponse -> {
                                    if (!clientResponse.statusCode().equals(HttpStatus.OK)) {
                                        log.error("Error while sending request to {}", address);
                                        return Mono.error(new SendingMessageException(message.getId(), address));
                                    }
                                    return clientResponse.bodyToMono(String.class);
                                }))
                .subscribe()
        );
    }

    @Override
    public void sendSynchronizationMessage(SyncMessage message) {
        generateFluxForSynchronizationMessage(
                message,
                getWithProtocol(zooKeeperRepository.getAllNodesHost(), replicaProtocol, PATH_FOR_SYNCHRONIZATION)
        ).subscribe();
    }

    @Override
    public void sendSynchronizationMessage(SyncMessage message, String topicName) {
        generateFluxForSynchronizationMessage(
                message,
                getWithProtocol(zooKeeperRepository.getReplicasAddress(topicName), replicaProtocol, PATH_FOR_SYNCHRONIZATION)
        ).subscribe();
    }

    private Flux<String> generateFluxForSynchronizationMessage(SyncMessage message, Collection<String> collection) {
        return Flux.fromIterable(collection)
                // нет смысла делать параллельно, так как кол-во реплик и кол-во получателей сообщений имеют разные порядки
                .flatMap(address -> {
                    message.setId(UUID.randomUUID().toString());
                    // т.к. все методы возвращают новые объекты не будет проблем (из-за использования одного объекта WebClient) при параллельной работе с методов startSend
                    return webClient.post()
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
                })
                // todo можно отправить админам письмо на почту, о недоступности данной ноды
                .doOnError(e -> ((SendingMessageException) e).getUrl());
    }
}