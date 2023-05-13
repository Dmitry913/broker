package org.wasend.broker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.dto.BrokerMessage;
import org.wasend.broker.dto.ConsumerMessageRegistry;
import org.wasend.broker.service.interfaces.BalancerPartitionService;
import org.wasend.broker.service.interfaces.MessageSender;
import org.wasend.broker.service.interfaces.MessageService;
import org.wasend.broker.service.mapper.MapperFactory;
import org.wasend.broker.service.model.MessageModel;
import org.wasend.broker.service.model.RegistryModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final MessageSender messageSender;
    private final QueueRepository queueRepository;
    private final ZooKeeperRepository zooKeeperRepository;
    private final MapperFactory mapperFactory;
    private final BalancerPartitionService balancerPartitionService;
    /**
     * Используется для равномерного распределения сообщений между партициями - балансировка нагрузки (информация только о master-node)
     * Формат - <topicName to <PartitionMasterId to countMessage>>
     */
    // todo раз в какой-то период обновлять информацию о партициях (в случае, если кол-во реплик и партиций будет отличаться)
    // todo данная переменная должна быть в queueRepository, так как она содержит в себе только те топики, где данный брокер владеет партицией и используется для балансировки сообщений в рамках этих топиков
    private final Map<String, Map<String, Integer>> topicToCountMessageNode = new HashMap<>();

    @Override
    // todo можно было бы распределить метод на 2:
    //  один для сообщений, где мы будем master-node
    //  другой для replica-message
    public void addMessage(MessageModel producerMessage) {
        log.info("\n\n\n{}\n\n\n", topicToCountMessageNode);
        // обновляем информацию по распределению сообщений между партиция
        if (producerMessage.isReplica()) {
            incrementCountMessage(producerMessage.getTopicName(), producerMessage.getPartitionId());
            queueRepository.addMessage(producerMessage);
            return;
        }
        boolean isNewTopic = !zooKeeperRepository.getAllTopicName().contains(producerMessage.getTopicName());
        // todo может быть ситуация, когда 2 разных брокера получили сообщения, в каждом из которых одинаковый топик,
        //  проверка не выявили наличие данного топика в системе, но когда мы попытаемся добавить, окажется, что другой брокер нас уже опередил и добавил
        //  вопрос - ЧТО ДЕЛАТЬ С ТАКОЙ СИТУАЦИЕЙ?
        // проверяем, создаётся ли новый топик
        if (isNewTopic) {
            // получаем наименее нагруженные узлы
            Set<String> partitionDirectoryNodes = balancerPartitionService.getDirectoryNodesForNewTopic();
            // добавляем текущий узел
            partitionDirectoryNodes.add(zooKeeperRepository.getCurrentDirectoryNode());
            // назначаем новому топику партиции(узлы, выбранные выше)
            Map<String, String> directoryToPartition = zooKeeperRepository.addNewTopicInfo(producerMessage.getTopicName(), partitionDirectoryNodes);
            topicToCountMessageNode.put(producerMessage.getTopicName(), directoryToPartition.values().stream().collect(Collectors.toMap(partitionId -> partitionId, partitionId -> 0)));
        }
        // балансировка нагрузки - распределения сообщений
        if (isNewTopic || isLightlyLoaded(producerMessage) || zooKeeperRepository.getCountPartition() == 1) {
            saveOnThisNode(producerMessage);
        } else {
            saveOnOtherNode(producerMessage);
        }
    }

    // todo можно было бы придумать различные вариант балансировки нагрузки (сейчас использован самый простой - количество сообщений, поступивших на узел)
    //  - можно было вообще находить узел с наименьшей загрузкой в зависимости от временного периода
    private boolean isLightlyLoaded(MessageModel producerMessage) {
        Set<String> myPartitionIds = zooKeeperRepository.getMyPartitionId(producerMessage.getTopicName());
        int countMessageInCurrentBroker = topicToCountMessageNode.get(producerMessage.getTopicName())
                .entrySet()
                .stream()
                .filter(entry -> myPartitionIds.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
        //debug
        log.info("countMessageInCurrentBroker = {}", countMessageInCurrentBroker);
        double averageCountMessage = topicToCountMessageNode
                .get(producerMessage.getTopicName())
                .values().stream()
                .mapToInt(Integer::intValue)
                .average().getAsDouble();
        // debug
        log.info("averageCountMessage = {}", averageCountMessage);
        return countMessageInCurrentBroker <= averageCountMessage;
    }

    private void saveOnOtherNode(MessageModel producerMessage) {
        Set<String> myPartitions = zooKeeperRepository.getMyPartitionId(producerMessage.getTopicName());
        // находим узел, на котором меньше всего сообщений
        String nodeDirectoryWithMinMessage = zooKeeperRepository.getDirectoryByPartition(
                topicToCountMessageNode.get(producerMessage.getTopicName())
                        .entrySet()
                        .stream()
                        .filter(entry -> !myPartitions.contains(entry.getKey()))
                        .min(Map.Entry.comparingByValue()).get().getKey());
        log.info("Delegate message(id={}) to <{}>", producerMessage.getId(), nodeDirectoryWithMinMessage);
        messageSender.delegateMessage(mapperFactory.mapTo(producerMessage, MessageModel.class, BrokerMessage.class), zooKeeperRepository.getHostByDirectory(nodeDirectoryWithMinMessage));
    }

    private void saveOnThisNode(MessageModel producerMessage) {
        List<String> myPartitions = new ArrayList<>(zooKeeperRepository.getMyPartitionId(producerMessage.getTopicName()));
        // todo можно выбирать партицию с наименьшим кол-вом сообщений или другой способ балансировки
        producerMessage.setPartitionId(myPartitions.get(new Random().nextInt(myPartitions.size())));
        // сохраняем сообщение в наше хранилище
        queueRepository.addMessage(producerMessage);
        incrementCountMessage(producerMessage.getTopicName(), producerMessage.getPartitionId());
        // отправляем сообщение для синхронизации другим брокерам
//        SyncMessage syncMessage = mapperFactory.mapTo(producerMessage, Message.class, SyncMessage.class);
        messageSender.sendSynchronizationProducerMessage(
                mapperFactory.mapTo(producerMessage, MessageModel.class, BrokerMessage.class),
                producerMessage.getTopicName());
    }


    @Override
    // todo всем репликам отправить копию (тут наверное лучше сделать синхронный механизм по умолчанию)
    public void registry(RegistryModel consumerMessage) {
        // сохраняем сообщение в наше хранилище
        queueRepository.registry(consumerMessage);
        // отправляем сообщение для синхронизации другим брокерам
        if (!consumerMessage.isReplica()) {
            messageSender.sendSynchronizationRegistryMessage(mapperFactory.mapTo(consumerMessage, RegistryModel.class, ConsumerMessageRegistry.class));
        }
    }

    @Override
    public Integer getCountMessage() {
        return queueRepository.getAllMessageCount();
    }

    private void incrementCountMessage(String topicName, String partitionId) {
        Map<String, Integer> partitionToCountMessage = topicToCountMessageNode.computeIfAbsent(topicName, k -> new HashMap<>());
        // todo можно подтащить данную информацию из queueRepository, а не обновлять её тут
        partitionToCountMessage.put(partitionId, partitionToCountMessage.getOrDefault(partitionId, 0) + 1);
    }

}
