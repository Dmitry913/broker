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
    // todo переделать - пусть получает партиции по топику из метода getPartitionByTopicName, а потом по партиции запрашивает название директории владельца
    Set<String> getReplicasAddress(String topicName);

    /**
     * Позволяет получить хосты всех существующих реплик
     */
    Set<String> getAllNodesHost();

    /**
     * Получаем название всех существующих топиков
     */
    Set<String> getAllTopicName();

    /**
     * @return все NodeId (название эфемерных директорий)
     */
    Set<String> getAllNodeDirectory();

    String getHostByDirectory(String directory);

    /**
     * @return количество партиций, которых должно быть для каждого топика (свойство системы)
     */
    int getCountPartition();

    /**
     * @return название эфемерного узла для текущей директории
     */
    String getCurrentDirectoryNode();

    /**
     * Возвращает идентификатор партиции, в которой текущий узел является мастером, по названию топика
     */
    Set<String> getMyPartitionId(String topicName);

    Map<String, String> addNewTopicInfo(String topicName, Set<String> partitionDirectory);

    Set<String> getPartitionsByDirectoryNode(String directoryNodeId);

//    Set<String> getPartitionWhereIAmReplica();
//
//    Set<String> getPartitionByTopicName(String topicName);

    String getTopicByPartitionId(String partitionId);

    /**
     * Метод должен транзакционно обновить информацию в MetaInfoZk.partitionIdToNodeDirectoryName в zooKeeper-е
     * @param addPartitionsOnCurrentInstance - те партиции, которые можно забрать к себе
     * @return id-партиций, которые instance добавит в обслуживание
     */
    Set<String> movePartitionToCurrentInstanceInTransaction(Set<String> addPartitionsOnCurrentInstance, String preferOwner);

}
