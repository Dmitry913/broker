package org.wasend.broker.dao.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;
import org.wasend.broker.dao.interfaces.DataStructure;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Repository
public class QueueRepositoryImpl implements QueueRepository {

    // todo перенести хранение в файл
    private final DataStructure<MessageModel> dataStructure;
    // todo перенести хранение в файл
    private final Set<String> urlConsumers;
    private final Map<String, Integer> topicNameToCountMasterMessage;
    private final Flux<MessageModel> messageExchanger;
    private FluxSink<MessageModel> publisher;
    @Value("seconds.before.sending")
    private int secondBeforeSending;
    private LocalDateTime currentTimeForSend;

    public QueueRepositoryImpl() {
        messageExchanger = Flux.<MessageModel>create(fluxSink -> publisher = fluxSink);
        dataStructure = new DataStructureProducerMessage();
        urlConsumers = new HashSet<>();
        // todo нужно обновлять значение, если мы вдруг стали мастером, для одной из наших реплик
        topicNameToCountMasterMessage = new HashMap<>();
        // т.к. поток асинхронный, нужно дождаться момента, пока переменная publisher не инициализируется
        while (publisher == null) {
        }
    }

    // todo можно завернуть в аспект, чтобы пушить как-то (подумать над механизмом тригера функции (может как-то через коллБек/аспекты/ивенты/флакс)
    @Override
    public void addMessage(MessageModel model) {
        // т.к. у нового сообщения дедлайн может быть более ранним, нужно отправлять именно его в сендерФункцию
        if (model.getDeadLine().isBefore(currentTimeForSend)) {
            currentTimeForSend = model.getDeadLine();
            publisher.next(model);
        }
        dataStructure.add(model);
        if (!model.isReplica()) {
            topicNameToCountMasterMessage.put(
                    model.getTopicName(),
                    topicNameToCountMasterMessage.getOrDefault(model.getTopicName(), 0) + 1);
        }
    }

    // todo нужно обработать кейс, когда тут висит в ожидание сообщение, которое ждёт своего времени обработки,
    //  а нам приходит новое сообщение, у которого дедлайн раньше текущего.
    @Override
    @EventListener
    public MessageModel getMessage() {
        // todo в качестве оптимизации можно было доставать события пачками и отслеживать у них дедлайн пачками
        MessageLinker linker = new MessageLinker(dataStructure.getMin());
        messageExchanger.subscribe(linker::setLink);
        /** Мониторит постоянно дедлайн линкера (своего рода вотчер).
         *  Реализовано так, потому что может прийти сообщение с более ранним дедлайном.
         *  При этом важно, чтобы сообщение не уходило на отправку раньше, чем определено в переменной secondBeforeSending.
         *  */
        // TODO Можно было бы поставить данный поток на ожидание, а в методе subscribe разбудить данный поток.
        while (linker.getLink().getDeadLine().isBefore(LocalDateTime.now().plusSeconds(secondBeforeSending))) {
        }
        return linker.getLink();
    }

    @Override
    public boolean registry(RegistryModel model) {
        return urlConsumers.add(model.getUrl());
    }

    @Override
    public int getMessagesCount(String topicName) {
        return topicNameToCountMasterMessage.get(topicName);
    }


    @Getter
    @Setter
    @AllArgsConstructor
    /** Нужен для привязки сообщений в методе Subscriber.onNext()*/
    private static class MessageLinker {
        private MessageModel link;
    }
}
