package org.wasend.broker.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.wasend.broker.dao.interfaces.QueueRepository;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.service.interfaces.BalancerPartitionService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wasend.broker.utils.HelpUtils.mapHostToUrl;

@Service
@RequiredArgsConstructor
public class BalancerPartitionServiceImpl implements BalancerPartitionService {

    private final ZooKeeperRepository zooKeeperRepository;
    private final WebClient webClient;
    private final QueueRepository queueRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    @Value("${replica.protocol}")
    // todo вынести в общий класс конфигов
    private String replicaProtocol;

    @Override
    public Set<String> getDirectoryNodesForNewTopic() {
        Map<String, Integer> directoryNodeToCountMessage = new HashMap<>();
        Flux.fromIterable(zooKeeperRepository.getAllNodeDirectory())
                .flatMap(directory ->
                        Mono.just(new Pair<>(
                                directory,
                                webClient.get()
                                        .uri(mapHostToUrl(zooKeeperRepository.getHostByDirectory(directory),replicaProtocol,"/v1/broker/countMessages"))
                                        // todo политика retry и обработка ошибок
                                        .retrieve().bodyToMono(Integer.TYPE)))
                )
                // todo возможно можно оптимизировать
                .subscribe(pair -> directoryNodeToCountMessage.put(pair.getFirst(), pair.getLast().block()));
        return directoryNodeToCountMessage.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                // todo сейчас для упрощения сделано так, что сообщение для нового топика всегда будет сохранятся на данном узле
                //  вне зависимости от его загруженности
                .limit(zooKeeperRepository.getCountPartition() - 1)
                .collect(Collectors.toSet());
    }

    @EventListener
    public void doOnNodeDown(List<String> livingNodes) {
        // находим название узла, который бы удалён
        List<String> cashNodes = new ArrayList<>(zooKeeperRepository.getAllNodeDirectory());
        if (cashNodes.size() <= livingNodes.size()) {
            // добавить новый узел в кеш
            applicationEventPublisher.publishEvent(new HashSet<>(livingNodes));
        }
        cashNodes.removeAll(livingNodes);
        String preferOwner = cashNodes.get(0);
        Set<String> lostInstancePartitions = zooKeeperRepository.getPartitionsByDirectoryNode(preferOwner);
        // по каждой партиции получить топик, и посмотреть есть ли партиция в данном топике за которую ответственен текущий экземпляр
        Map<String, String> lostTopicToPartitionId = lostInstancePartitions.stream().collect(Collectors.toMap(zooKeeperRepository::getTopicByPartitionId, partitionId -> partitionId));
        // идентификаторы партиций, для которых упавший брокер был мастером и копии которых находятся на текущем экземпляре
        Set<String> addPartitionsOnCurrentInstance =
                // оставляем только те партиции, топики которых были потеряны
                lostTopicToPartitionId.keySet()
                .stream()
                .collect(Collectors.toMap(topicName -> topicName, zooKeeperRepository::getMyPartitionId))
                .entrySet()
                .stream()
                // оставляем только те партиции, топики которых содержатся на текущем узле
                .filter(entry -> entry.getValue() != null)
                // получаем id партиции, которая может быть передана в обработку текущему экземпляру
                .map(entry -> lostTopicToPartitionId.get(entry.getKey()))
                .collect(Collectors.toSet());
        // является ли текущий инстанс репликой для каких-то партиций, где мастером был упавший экземпляр
        // (Примечание: так как сейчас кол-во каждая нода-партиция содержит в себе все реплики других партиций в рамках одного топика, то,
        // чтобы определить, содержится ли у нас копия, нужно всего лишь понять, являюсь ли я мастер-партицией для этого топика)
        if (!addPartitionsOnCurrentInstance.isEmpty()) {
            Set<String> newPartition = zooKeeperRepository.movePartitionToCurrentInstanceInTransaction(addPartitionsOnCurrentInstance, preferOwner);
            if (!newPartition.isEmpty()) {
                // запуск обработки сообщений, которыми теперь управляет данный instance
                Flux.fromIterable(newPartition)
                        .subscribe(queueRepository::movePartitionToProcessing);
            }
        }
        // удалить из кеша упавшие узлы
        applicationEventPublisher.publishEvent(new HashSet<>(livingNodes));
    }

    @AllArgsConstructor
    @Getter
    private final class Pair<T, K> {
        T first;
        K last;
    }
}
