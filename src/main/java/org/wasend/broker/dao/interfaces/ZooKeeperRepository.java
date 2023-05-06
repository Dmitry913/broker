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
    Set<String> getAllNodesAddress();
}