package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.interfaces.PartitionService;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;
import org.wasend.broker.service.model.SyncMessage;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSender messageSender;
    private final QueueRepository queueRepository;
    private final ZooKeeperRepository zooKeeperRepository;
    private final MapperFactory mapperFactory;
    private final PartitionService partitionService;
    /**
     * Используется для равномерного распределения сообщений между партициями - балансировка нагрузки
     * Формат - <topicName to <PartitionAddress to countMessage>>
     */
    // todo раз в какой-то период обновлять информацию о партициях
    private final Map<String, Map<String, Integer>> topicToCountMessageNode;

    @Override
    // todo можно было бы распределить метод на 2:
    //  один для сообщений, где мы будем master-node
    //  другой для replica-message
    public void addMessage(MessageModel producerMessage) {
        boolean isNewTopic = !zooKeeperRepository.getAllTopicName().contains(producerMessage.getTopicName());
        // todo может быть ситуация, когда 2 разных брокера получили сообщения, в каждом из которых одинаковый топик,
        //  проверка не выявили наличие данного топика в системе, но когда мы попытаемся добавить, окажется, что другой брокер нас уже опередил и добавил
        //  вопрос - ЧТО ДЕЛАТЬ С ТАКОЙ СИТУАЦИЕЙ?
        // проверяем, создаётся ли новый топик
        if (isNewTopic) {
            // получаем наименее нагруженные узлы
            Set<String> partitionNodes = partitionService.getNodesForNewTopic();
            // назначаем новому топику партиции(узлы, выбранные выше)
            zooKeeperRepository.addNewTopicInfo(producerMessage.getTopicName(), partitionNodes);
            topicToCountMessageNode.put(producerMessage.getTopicName(), partitionNodes.stream().collect(Collectors.toMap(node -> node, node -> 0)));
        }
        // обновляем информацию по распределению сообщений между партиция
        if (producerMessage.isReplica()) {
            Map<String, Integer> partitionToCountMessage = topicToCountMessageNode.get(producerMessage.getTopicName());
            partitionToCountMessage.put(producerMessage.getMasterNode(), partitionToCountMessage.getOrDefault(producerMessage.getMasterNode(), 0) + 1);
        }
        // балансировка нагрузки - распределения сообщений
        if (isNewTopic || isLightlyLoaded(producerMessage)) {
            saveOnThisNode(producerMessage);
        } else {
            saveOnOtherNode(producerMessage);
        }
    }

    // todo можно было бы придумать различные вариант балансировки нагрузки (сейчас использован самый простой)
    //  - можно было вообще находить узел с наименьшей загрузкой в зависимости от временного периода
    private boolean isLightlyLoaded(MessageModel producerMessage) {
        int countMessageInCurrentBroker = queueRepository.getMessagesCount(producerMessage.getTopicName());
        double averageCountMessage = topicToCountMessageNode
                .get(producerMessage.getTopicName())
                .values().stream()
                .mapToInt(Integer::intValue)
                .average().getAsDouble();
        return countMessageInCurrentBroker < averageCountMessage;
    }

    private void saveOnOtherNode(MessageModel producerMessage) {
        // находим узел, на котором меньше всего сообщений
        String nodeWithMinMessage = topicToCountMessageNode.get(producerMessage.getTopicName())
                .entrySet()
                .stream()
                .min(Map.Entry.comparingByValue()).get().getKey();
        messageSender.delegateMessage(producerMessage, nodeWithMinMessage);
    }

    private void saveOnThisNode(MessageModel producerMessage) {
        // сохраняем сообщение в наше хранилище
        queueRepository.addMessage(producerMessage);
        // отправляем сообщение для синхронизации другим брокерам
        producerMessage.setMasterNode(zooKeeperRepository.getCurrentNodeId());
        messageSender.sendSynchronizationMessage(mapperFactory.mapTo(producerMessage, SyncMessage.class), producerMessage.getTopicName());
    }


    @Override
    // todo всем репликам отправить копию (тут наверное лучше сделать синхронный механизм по умолчанию)
    public void registry(RegistryModel consumerMessage) {
        // сохраняем сообщение в наше хранилище
        queueRepository.registry(consumerMessage);
        // отправляем сообщение для синхронизации другим брокерам
        messageSender.sendSynchronizationMessage(mapperFactory.mapTo(consumerMessage, SyncMessage.class));
    }

    @Override
    public Integer getCountMessage() {
        return queueRepository.getAllMessageCount();
    }
}
