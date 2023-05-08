package org.wasend.broker.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;
import org.wasend.broker.dao.entity.TopicInfo;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.eventObjects.MetaInfo;
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
    /**
     * Название эфемерной ноды для текущего инстанса
     */
    private final String currentNodeId;
    private MetaInfoZK rootInfo;
    private int rootDirectoryVersion;
    // Возможно стоит сделать синхронной(блокирующей)?
    private Map<String, NodeInfo> directoryToNodesInfo;

    @Autowired
    public ZooKeeperRepositoryImpl(CuratorZooKeeper curatorZooKeeper) throws Exception {
        this.curatorZooKeeper = curatorZooKeeper;
        this.currentNodeId = curatorZooKeeper.instanceNodeId();
        MetaInfo metaInfo = curatorZooKeeper.getRootInfo();
        rootInfo = metaInfo.getMetaInfoZK();
        rootDirectoryVersion = metaInfo.getRootDirectoryVersion();
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
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
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
        try {
            curatorZooKeeper.updateMetaInfo(rootInfo, rootDirectoryVersion);
        } catch (Exception e) {

        }
        return directoryToPartition;
    }

    @Override
    public Set<String> getPartitionsByDirectoryNode(String directoryNodeId) {
        return rootInfo.getPartitionIdToNodeDirectoryName().entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(directoryNodeId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public String getTopicByPartitionId(String partitionId) {
        return rootInfo.getTopicNameToInfo().values()
                .stream()
                .filter(topicInfo -> topicInfo.getPartitionsId().contains(partitionId))
                .map(TopicInfo::getName)
                .findAny().orElse(null);
    }

    @Override
    public Set<String> movePartitionToCurrentInstanceInTransaction(Set<String> addPartitionsOnCurrentInstance, String preferOwner) {
        Set<String> copyPartitionsId = new HashSet<>(addPartitionsOnCurrentInstance);
        // отправить партиции на привязку к текущему узлу
        while (!copyPartitionsId.isEmpty()) {
            log.info("Attempt to link Partitions:{} with node:{} ", copyPartitionsId, currentNodeId);
            MetaInfoZK copyMetaInfo = new MetaInfoZK(rootInfo);
            copyPartitionsId.forEach(partitionId -> copyMetaInfo.getPartitionIdToNodeDirectoryName().put(partitionId, currentNodeId));
            if (curatorZooKeeper.updateMetaInfo(copyMetaInfo, rootDirectoryVersion)) {
                log.error("Attempt success");
                return copyPartitionsId;
            } else {
                // если привязка не удалась, нужно исключить из списка партиции, которые успели привязать другие брокеры и отправить запрос заново
                copyPartitionsId = copyPartitionsId.stream()
                        .filter(partitionId -> rootInfo.getPartitionIdToNodeDirectoryName().get(partitionId).equals(preferOwner))
                        .collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }


    @EventListener
    public void updateMetaData(MetaInfo metaInfo) {
        this.rootInfo = metaInfo.getMetaInfoZK();
        this.rootDirectoryVersion = metaInfo.getRootDirectoryVersion();
    }

    @EventListener
    // стереть информацию об удалённом узле из zookeeperRepository.directoryToNodeInfo
    // если добавился новый узел, то надо добавить информацию в zookeeperRepository.directoryToNodeInfo
    public void actualizeDirectoryToNodesInfo(List<String> livingNodes) {
        Set<String> unchangedNodes = new HashSet<>(directoryToNodesInfo.keySet());
        // пересечение - те директории, которые никак не изменились
        unchangedNodes.retainAll(livingNodes);
        Set<String> newNodes = new HashSet<>(livingNodes);
        newNodes.removeAll(unchangedNodes);
        Set<NodeInfo> resultNode = directoryToNodesInfo.values()
                .stream()
                .filter(nodeInfo -> unchangedNodes.contains(nodeInfo.getNodeId()))
                .collect(Collectors.toSet());
        Flux.fromIterable(newNodes).flatMap(directoryName -> Mono.just(curatorZooKeeper.getNodeInfoByDirectory(directoryName))).subscribe(resultNode::add);
        updateReplicaHosts(resultNode);
    }


    private String getAddressFromHostAndPort(NodeInfo nodeInfo) {
        return nodeInfo.getHost() + ":" + nodeInfo.getPort();
    }

    public void updateReplicaHosts(Collection<NodeInfo> nodesInfo) {
        this.directoryToNodesInfo = nodesInfo.stream()
                .peek(nodeInfo -> log.info("Node{} detected and added", nodeInfo.getNodeId()))
                .collect(Collectors.toMap(NodeInfo::getNodeId, node -> node));
    }
}
