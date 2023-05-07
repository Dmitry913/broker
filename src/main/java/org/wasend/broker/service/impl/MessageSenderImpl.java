package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.dto.ConsumerMessagePayload;
import org.wasend.broker.exception.SendingMessageException;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.model.Message;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.SyncMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import static org.wasend.broker.utils.HelpUtils.getWithProtocol;
import static org.wasend.broker.utils.HelpUtils.mapHostToUrl;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSenderImpl implements MessageSender {

    private final static String PATH_FOR_SYNCHRONIZATION = "/v1/broker/updateReplica";
    private final WebClient webClient;
    private final QueueRepository queueRepository;
    private final ZooKeeperRepository zooKeeperRepository;
    @Value("replica.protocol")
    private final String replicaProtocol;

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
                        generateRequest(
                                address,
                                new ConsumerMessagePayload(message.getPayload()),
                                String.class))
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
    public void delegateMessage(MessageModel producerMessage, String hostNode) {
        String url = mapHostToUrl(hostNode, replicaProtocol, "/v1/broker/addMessage");
        generateRequest(url, producerMessage, String.class).toFuture()
                .thenAccept(response -> log.info("Message sending success. Response - " + response));
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
                .flatMap(address ->
                        // т.к. все методы возвращают новые объекты не будет проблем (из-за использования одного объекта WebClient) при параллельной работе с методов startSend
                        generateRequest(address, message, String.class)
                )
                // todo можно отправить админам письмо на почту, о недоступности данной ноды
                .doOnError(e -> ((SendingMessageException) e).getUrl());
    }

    private <T> Mono<T> generateRequest(String url, Message message, Class<T> typeResponse) {
        return webClient.post()
                .uri(url)
                .bodyValue(message)
                // todo можно было бы в хедеры прикладывать ip-адрес системы
                //  (внутри шифта?? тогда получается надо ip-adress ноды прикладывать, то есть сервера)
                .header("service", "server-id")
                // todo определить политику retry??
                .exchangeToMono(clientResponse -> {
                    if (!clientResponse.statusCode().equals(HttpStatus.OK)) {
                        log.error("Error while sending synchronization message to {}", url);
                        return Mono.error(new SendingMessageException(message.getId(), url));
                    }
                    return clientResponse.bodyToMono(typeResponse);
                });
    }
}