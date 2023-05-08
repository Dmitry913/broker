package org.wasend.broker.dao.interfaces;

import org.wasend.broker.dao.entity.MetaInfoZK;

import java.util.Map;
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

    String getCurrentDirectoryNode();

    /**
     * Возвращает идентификатор партиции, в которой текущий узел является мастером, по названию топика
     */
    String getMyPartitionId(String topicName);

    Map<String, String> addNewTopicInfo(String topicName, Set<String> partitionDirectory);

}
