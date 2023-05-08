package org.wasend.broker.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;
import org.wasend.broker.dao.entity.TopicInfo;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.zookeeper.CuratorZooKeeper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@Slf4j
public class ZooKeeperRepositoryImpl implements ZooKeeperRepository {

    private final CuratorZooKeeper curatorZooKeeper;
    private MetaInfoZK rootInfo;
    // Возможно стоит сделать синхронной(блокирующей)?
    private Map<String, NodeInfo> directoryToNodesInfo;
    @Value("zooKeeper.current.nodeId")
    private String currentNodeId;

    @Autowired
    public ZooKeeperRepositoryImpl(CuratorZooKeeper curatorZooKeeper) throws Exception {
        this.curatorZooKeeper = curatorZooKeeper;
        rootInfo = curatorZooKeeper.getRootInfo();
        initNodesInfo();
    }

    private void initNodesInfo() {
        Flux.fromIterable(
                        // получаем информацию обо всех существующих nodes
                        rootInfo.getTopicNameToInfo().values()
                                // получаем наименование директорий всех узлов
                                .stream()
                                .flatMap(
                                        topicInfo -> topicInfo.getPartitionsId()
                                                .stream()
                                                .map(partitionId -> rootInfo.getPartitionIdToNodeDirectoryName().get(partitionId))
                                )
                                .collect(Collectors.toSet())
                )
                .flatMap(directoryName -> Mono.just(curatorZooKeeper.getNodeInfoByDirectory(directoryName)))
                .collectList()
                .subscribe(this::updateReplicaHosts);
    }

    @Override
    public Set<String> getReplicasAddress(String topicName) {
        // получаем nodeId(название относительной директории) всех реплик
        return rootInfo.getTopicNameToInfo().get(topicName).getPartitionsId()
                //получаем информацию по каждой node из директорий
                .stream()
                .map(partitionId -> rootInfo.getPartitionIdToNodeDirectoryName().get(partitionId))
                .map(directoryName -> directoryToNodesInfo.get(directoryName))
                .map(this::getAddressFromHostAndPort)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllNodesHost() {
        return directoryToNodesInfo.values().stream().map(this::getAddressFromHostAndPort).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllTopicName() {
        return rootInfo.getTopicNameToInfo().keySet();
    }

    @Override
    public Set<String> getAllNodeDirectory() {
        return directoryToNodesInfo.keySet();
    }

    @Override
    public String getHostByDirectory(String directory) {
        return getAddressFromHostAndPort(directoryToNodesInfo.get(directory));
    }

    @Override
    public int getCountPartition() {
        return rootInfo.getCountPartition();
    }

    @Override
    public String getCurrentDirectoryNode() {
        return currentNodeId;
    }

    @Override
    // todo лучше как-то поменять формат или ещё что-то, для того чтобы не приходилось каждый раз ревёрсить мапу, т.к. это затратная операция
    public String getMyPartitionId(String topicName) {
        Set<String> topicPartitionId = rootInfo.getTopicNameToInfo().get(topicName).getPartitionsId();
        return rootInfo.getPartitionIdToNodeDirectoryName()
                .entrySet()
                .stream()
                .filter(entry -> topicPartitionId.contains(entry.getKey()))
                .filter(entry -> entry.getValue().equals(currentNodeId))
                .findAny()
                .orElseThrow(() -> {
                    log.error("Failed to find master-partitionId for this node in topic-" + topicName);
                    return new RuntimeException();
                }).getKey();
    }

    @Override
    public Map<String, String> addNewTopicInfo(String topicName, Set<String> partitionNodeDirectory) {
        // генерируем айдишники для новых партиций
        Set<String> newPartitions = IntStream
                .range(0, rootInfo.getCountPartition())
                .mapToObj(i -> "partition" + UUID.randomUUID())
                .collect(Collectors.toSet());
//        Set<String> directoryNewPartition = directoryToNodesInfo.values().stream()
//                .map(NodeInfo::getNodeId)
//                .filter(partitionNodeDirectory::contains)
//                .collect(Collectors.toSet());
        // привязываем топик к новым партициям
        rootInfo.addNewTopic(new TopicInfo(topicName, newPartitions));
        // связываем партиции с узлами
        Map<String, String> partitionToDirectory = new HashMap<>();
        Map<String, String> directoryToPartition = new HashMap<>();
        Iterator<String> iteratorByDirectory = partitionNodeDirectory.iterator();
        for (String partitionId : newPartitions) {
            partitionToDirectory.put(partitionId, iteratorByDirectory.next());
            directoryToPartition.put(iteratorByDirectory.next(), partitionId);
        }
        rootInfo.linkNewPartition(partitionToDirectory);
        curatorZooKeeper.updateMetaInfo(rootInfo);
        return directoryToPartition;
    }

    @EventListener
    public void updateMetaData(MetaInfoZK metaInfoZK) {
        this.rootInfo = metaInfoZK;
    }

    private String getAddressFromHostAndPort(NodeInfo nodeInfo) {
        return nodeInfo.getHost() + ":" + nodeInfo.getPort();
    }

    public void updateReplicaHosts(Collection<NodeInfo> nodesInfo) {
        this.directoryToNodesInfo = nodesInfo.stream().collect(Collectors.toMap(NodeInfo::getNodeId, node -> node));
    }
}
