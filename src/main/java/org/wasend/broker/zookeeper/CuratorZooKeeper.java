package org.wasend.broker.zookeeper;

import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;
import org.wasend.broker.eventObjects.MetaInfo;

/**
 * Класс для работы с zooKeeper
 */
public interface CuratorZooKeeper {

    /**
     * Возвращает информацию из корневой директории
     */
    MetaInfo getRootInfo() throws Exception;

    /**
     * Позволяет получить информацию об узле по директории
     */
    NodeInfo getNodeInfoByDirectory(String directory);

    boolean updateMetaInfo(MetaInfoZK metaInfo, int version);

    String instanceNodeId();

}
