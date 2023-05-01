package org.wasend.broker.dao.interfaces;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Класс для работы с метаИнформацией (адреса реплик; кто master, а кто slave)
 */
public interface MetaInfoRepository {

    /**
     * Позволяет получить хосты всех реплик для данного узла
     */
    Set<String> getReplicasAddress(String topicName);

    Set<String> getAllNodesAddress();
}
