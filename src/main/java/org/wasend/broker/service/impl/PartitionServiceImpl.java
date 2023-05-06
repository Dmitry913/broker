package org.wasend.broker.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.service.interfaces.PartitionService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.wasend.broker.utils.HelpUtils.getWithProtocol;

@Service
@RequiredArgsConstructor
public class PartitionServiceImpl implements PartitionService {

    private final ZooKeeperRepository zooKeeperRepository;
    private final WebClient webClient;
    @Value("${replica.protocol}")
    // todo вынести в общий класс конфигов
    private String replicaProtocol;

    @Override
    public Set<String> getNodesForNewTopic() {
        Map<String, Integer> hostToCountMessage = new HashMap<>();
        Flux.fromIterable(getWithProtocol(zooKeeperRepository.getAllNodesHost(), replicaProtocol, "/v1/broker/countMessages"))
                .flatMap(address ->
                        Mono.just(new Pair<>(
                                address,
                                webClient.get()
                                        .uri(address)
                                        // todo политика retry и обработка ошибок
                                        .retrieve().bodyToMono(Integer.TYPE)))
                )
                // todo возможно можно оптимизировать
                .subscribe(pair -> hostToCountMessage.put(pair.getFirst(), pair.getLast().block()));
        return hostToCountMessage.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                // todo сейчас для упрощения сделано так, что сообщение для нового топика всегда будет сохранятся на данном узле
                //  вне зависимости от его загруженности
                .limit(zooKeeperRepository.getCountPartition() - 1)
                .collect(Collectors.toSet());
    }

    @AllArgsConstructor
    @Getter
    private final class Pair<T, K> {
        T first;
        K last;
    }
}
