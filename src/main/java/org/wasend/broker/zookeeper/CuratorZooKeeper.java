package org.wasend.broker.zookeeper;

import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;

/**
 * Класс для работы с zooKeeper
 */
public interface CuratorZooKeeper {

    /**
     * Возвращает информацию из корневой директории
     */
    MetaInfoZK getRootInfo() throws Exception;

    /**
     * Позволяет получить информацию об узле по директории
     */
    NodeInfo getNodeInfoByDirectory(String directory);
}
