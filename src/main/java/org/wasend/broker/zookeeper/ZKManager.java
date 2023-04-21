package org.wasend.broker.zookeeper;

import org.apache.zookeeper.KeeperException;

public interface ZKManager {

    void create(String path, byte[] data) throws InterruptedException, KeeperException;

    String getZNodeData(String path, boolean watchFlag) throws InterruptedException, KeeperException;

    void update(String path, byte[] data) throws InterruptedException, KeeperException;

    void delete(String path) throws InterruptedException, KeeperException;
}
