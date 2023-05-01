package org.wasend.broker.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.interfaces.MetaInfoRepository;
import org.wasend.broker.zookeeper.CuratorZooKeeper;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class MetaInfoRepositoryImpl implements MetaInfoRepository {

    private final CuratorZooKeeper curatorZooKeeper;
    // todo данные должны заполнится из zooKepper и постоянно поддерживаться в актуальном состоянии
    private final Set<String> replicaHost;
    private final MetaInfoZK rootInfo;

    @Autowired
    public MetaInfoRepositoryImpl(CuratorZooKeeper curatorZooKeeper) {
        this.curatorZooKeeper = curatorZooKeeper;
        replicaHost = new HashSet<>();
        rootInfo = curatorZooKeeper.getRootInfo();
    }

    @Override
    public Set<String> getReplicasAddress(String topicName) {
        return replicaHost;
    }

    @Override
    public Set<String> getAllNodesAddress() {
        return curatorZooKeeper.getAllNodes().stream()
                .map(nodeInfo -> "http://" + nodeInfo.getHost() + ":" + nodeInfo.getPort())
                .collect(Collectors.toSet());
    }
}
