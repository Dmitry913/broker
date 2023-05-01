package org.wasend.broker.zookeeper;

import org.wasend.broker.dao.entity.MetaInfoZK;
import org.wasend.broker.dao.entity.NodeInfo;

import java.util.Set;

/**
 * Класс для работы с zooKeeper
 */
public interface CuratorZooKeeper {

    /**
     * Возвращает информацию из корневой директории
     */
    MetaInfoZK getRootInfo();

    /**
     * Возвращает информацию из всех дочерних директорий
     */
    Set<NodeInfo> getAllNodes();
}
