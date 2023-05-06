package org.wasend.broker.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;
import org.wasend.broker.dao.entity.TopicInfo;
import org.wasend.broker.dao.interfaces.ZooKeeperRepository;
import org.wasend.broker.zookeeper.CuratorZooKeeper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ZooKeeperRepositoryImpl implements ZooKeeperRepository {

    private final CuratorZooKeeper curatorZooKeeper;
    private final MetaInfoZK rootInfo;
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
                                .stream().flatMap(topicInfo -> topicInfo.getPartitionMasterNode().stream()).collect(Collectors.toSet())
                )
                .flatMap(directoryName -> Mono.just(curatorZooKeeper.getNodeInfoByDirectory(directoryName)))
                .collectList()
                .subscribe(this::updateReplicaHosts);
    }

    @Override
    public Set<String> getReplicasAddress(String topicName) {
        // получаем nodeId(название относительной директории) всех реплик
        return rootInfo.getTopicNameToInfo().get(topicName).getPartitionMasterNode()
                //получаем информацию по каждой node из директорий
                .stream()
                .map(directory -> directoryToNodesInfo.get(directory))
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
    public int getCountPartition() {
        return rootInfo.getCountPartition();
    }

    @Override
    public String getCurrentNodeId() {
        return currentNodeId;
    }

    @Override
    public void addNewTopicInfo(String topicName, Set<String> partitionHost) {
        Set<String> directoryNewPartition = directoryToNodesInfo.values().stream()
                .filter(info -> partitionHost.contains(info.getHost()))
                .map(NodeInfo::getNodeId)
                .collect(Collectors.toSet());
        rootInfo.addNewTopic(new TopicInfo(topicName, directoryNewPartition));
        curatorZooKeeper.updateMetaInfo(rootInfo);
    }

    private String getAddressFromHostAndPort(NodeInfo nodeInfo) {
        return nodeInfo.getHost() + ":" + nodeInfo.getPort();
    }

    public void updateReplicaHosts(Collection<NodeInfo> nodesInfo) {
        this.directoryToNodesInfo = nodesInfo.stream().collect(Collectors.toMap(NodeInfo::getNodeId, node -> node));
    }
}
