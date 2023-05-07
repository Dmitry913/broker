package org.wasend.broker.dao.interfaces;

import java.util.Set;

/**
 * Класс для работы с метаИнформацией (адреса реплик; кто master, а кто slave)
 */
public interface ZooKeeperRepository {
    /**
     * Позволяет получить хосты всех реплик для данного топика
     */
    Set<String> getReplicasAddress(String topicName);

    /**
     * Позволяет получить хосты всех существующих реплик
     */
    Set<String> getAllNodesHost();

    /**
     * Получаем название всех существующих топиков
     */
    Set<String> getAllTopicName();

    Set<String> getAllNodeDirectory();

    String getHostByDirectory(String directory);

    int getCountPartition();

    String getCurrentNodeId();

    void addNewTopicInfo(String topicName, Set<String> partitionDirectory);
}
